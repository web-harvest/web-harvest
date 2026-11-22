#!/usr/bin/env python3
"""Correlate shaded archive classes with resolved runtime dependency JARs.

A class-path match is evidence of a candidate origin, not by itself proof of
byte identity. Exact class-byte SHA-256 matches are reported separately.
Relocated Jetty classes in the CLI remain a dedicated, explicitly bounded
special case handled by shaded_distribution_inventory.py.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path, PurePosixPath
import zipfile


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse_properties(data: bytes) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in data.decode("ISO-8859-1").splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", "!")) or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def coordinates_from_repository_path(path: Path, root: Path) -> str | None:
    try:
        parts = path.relative_to(root).parts
    except ValueError:
        return None
    if len(parts) < 4 or not parts[-1].endswith(".jar"):
        return None
    group = ".".join(parts[:-3])
    artifact, version = parts[-3], parts[-2]
    if not group or not artifact or not version:
        return None
    return f"{group}:{artifact}:{version}:jar"


def inspect_dependency_jar(path: Path, root: Path) -> dict:
    digest = sha256(path.read_bytes())
    embedded_coordinates: set[str] = set()
    classes: dict[str, str] = {}
    with zipfile.ZipFile(path) as jar:
        names = sorted(set(jar.namelist()))
        for name in names:
            if name.startswith("META-INF/maven/") and name.endswith("/pom.properties"):
                props = parse_properties(jar.read(name))
                group, artifact, version = (
                    props.get("groupId"), props.get("artifactId"), props.get("version")
                )
                if group and artifact and version:
                    embedded_coordinates.add(f"{group}:{artifact}:{version}:jar")
            if (name.endswith(".class") and not name.endswith("module-info.class")
                   ):
                classes[name] = sha256(jar.read(name))
    path_coordinate = coordinates_from_repository_path(path, root)
    coordinates = sorted(embedded_coordinates)
    primary = path_coordinate or (coordinates[0] if len(coordinates) == 1 else None)
    return {
        "path": path.relative_to(root).as_posix(),
        "sha256": digest,
        "coordinate": primary or f"unidentified:{path.relative_to(root).as_posix()}",
        "path_coordinate": path_coordinate,
        "embedded_coordinates": coordinates,
        "class_count": len(classes),
        "classes": classes,
    }


def is_cli_relocated_jetty(class_path: str) -> bool:
    normalized = class_path
    if normalized.startswith("META-INF/versions/"):
        parts = normalized.split("/", 3)
        normalized = parts[3] if len(parts) == 4 else normalized
    return normalized.startswith("org/htmlunit/jetty/")


def archive_coordinates(archive: zipfile.ZipFile) -> set[str]:
    found: set[str] = set()
    for name in archive.namelist():
        if not (name.startswith("META-INF/maven/") and name.endswith("/pom.properties")):
            continue
        props = parse_properties(archive.read(name))
        group, artifact, version = props.get("groupId"), props.get("artifactId"), props.get("version")
        if group and artifact and version:
            found.add(f"{group}:{artifact}:{version}:jar")
    return found


def audit(archive_path: Path, dependency_dir: Path, distribution: str) -> dict:
    dependencies = [
        inspect_dependency_jar(path, dependency_dir)
        for path in sorted(dependency_dir.rglob("*.jar"))
        if path.is_file()
    ]
    if not dependencies:
        raise ValueError(f"No dependency JARs found below {dependency_dir}")

    candidates: dict[str, list[dict]] = defaultdict(list)
    dependency_components: dict[str, dict] = {}
    for dependency in dependencies:
        coordinate = dependency["coordinate"]
        component = dependency_components.setdefault(coordinate, {
            "coordinate": coordinate,
            "dependency_jars": [],
            "dependency_jar_sha256": [],
            "dependency_class_count": 0,
            "matched_archive_class_count": 0,
            "unique_exact_byte_match_count": 0,
            "unique_path_match_bytes_differ_count": 0,
            "ambiguous_class_count": 0,
            "metadata_in_shaded_archive": False,
        })
        component["dependency_jars"].append(dependency["path"])
        component["dependency_jar_sha256"].append(dependency["sha256"])
        component["dependency_class_count"] += dependency["class_count"]
        for class_path, class_sha in dependency["classes"].items():
            candidates[class_path].append({
                "coordinate": coordinate,
                "dependency_jar": dependency["path"],
                "dependency_jar_sha256": dependency["sha256"],
                "class_sha256": class_sha,
            })

    with zipfile.ZipFile(archive_path) as archive:
        archive_bytes = archive_path.read_bytes()
        shaded_coordinates = archive_coordinates(archive)
        archive_classes = {}
        for name in sorted(set(archive.namelist())):
            if (name.endswith(".class") and not name.endswith("module-info.class")
                   ):
                archive_classes[name] = sha256(archive.read(name))

    for component in dependency_components.values():
        component["metadata_in_shaded_archive"] = component["coordinate"] in shaded_coordinates

    class_results = []
    counts: Counter[str] = Counter()
    for class_path, class_sha in archive_classes.items():
        matches = candidates.get(class_path, [])
        exact = [candidate for candidate in matches if candidate["class_sha256"] == class_sha]
        if distribution == "cli" and is_cli_relocated_jetty(class_path):
            # HtmlUnit relocates Jetty classes into this namespace. Preserve any
            # container-JAR candidates, but do not misattribute them to HtmlUnit;
            # the pinned Jetty module mapping remains the origin evidence.
            status = "special_case_cli_relocated_jetty"
            selected = []
        elif len(exact) == 1:
            status = "unique_exact_byte_match"
            selected = exact
        elif len(exact) > 1:
            status = "ambiguous_exact_byte_match"
            selected = exact
        elif len(matches) == 1:
            status = "unique_path_match_bytes_differ"
            selected = matches
        elif len(matches) > 1:
            status = "ambiguous_path_match_bytes_differ"
            selected = matches
        else:
            status = "unmatched"
            selected = []
        counts[status] += 1

        selected_coordinates = sorted({candidate["coordinate"] for candidate in selected})
        for coordinate in selected_coordinates:
            component = dependency_components[coordinate]
            component["matched_archive_class_count"] += 1
            if status == "unique_exact_byte_match":
                component["unique_exact_byte_match_count"] += 1
            elif status == "unique_path_match_bytes_differ":
                component["unique_path_match_bytes_differ_count"] += 1
            elif status.startswith("ambiguous_"):
                component["ambiguous_class_count"] += 1

        class_results.append({
            "archive_class": class_path,
            "archive_class_sha256": class_sha,
            "status": status,
            "candidates": [
                {
                    "coordinate": candidate["coordinate"],
                    "dependency_jar": candidate["dependency_jar"],
                    "dependency_jar_sha256": candidate["dependency_jar_sha256"],
                    "class_sha256": candidate["class_sha256"],
                }
                for candidate in matches
            ],
        })

    # Include shaded metadata coordinates that were not found among copied runtime JARs.
    for coordinate in sorted(shaded_coordinates):
        if coordinate not in dependency_components:
            dependency_components[coordinate] = {
                "coordinate": coordinate,
                "dependency_jars": [],
                "dependency_jar_sha256": [],
                "dependency_class_count": 0,
                "matched_archive_class_count": 0,
                "unique_exact_byte_match_count": 0,
                "unique_path_match_bytes_differ_count": 0,
                "ambiguous_class_count": 0,
                "metadata_in_shaded_archive": True,
            }

    for component in dependency_components.values():
        coordinate_parts = component["coordinate"].split(":")
        is_cli_jetty_special_case = (
            distribution == "cli"
            and len(coordinate_parts) >= 3
            and coordinate_parts[0].startswith("org.eclipse.jetty")
            and coordinate_parts[2] == "12.1.15"
        )
        component["metadata_only_or_unmatched"] = (
            component["metadata_in_shaded_archive"]
            and component["matched_archive_class_count"] == 0
            and not is_cli_jetty_special_case
        )

    return {
        "schema": "webharvest.shaded-class-origin-audit.v1",
        "distribution": distribution,
        "archive": {
            "file": archive_path.name,
            "size_bytes": len(archive_bytes),
            "sha256": sha256(archive_bytes),
            "class_count": len(archive_classes),
        },
        "dependency_set": {
            "root": dependency_dir.name,
            "jar_count": len(dependencies),
            "total_dependency_class_entries": sum(item["class_count"] for item in dependencies),
            "jars_without_embedded_maven_coordinates": sum(
                not item["embedded_coordinates"] for item in dependencies
            ),
            "jar_inventory": [
                {key: value for key, value in item.items() if key != "classes"}
                for item in dependencies
            ],
        },
        "summary": {
            "archive_class_count": len(archive_classes),
            "unique_exact_byte_match": counts["unique_exact_byte_match"],
            "unique_path_match_bytes_differ": counts["unique_path_match_bytes_differ"],
            "ambiguous_exact_byte_match": counts["ambiguous_exact_byte_match"],
            "ambiguous_path_match_bytes_differ": counts["ambiguous_path_match_bytes_differ"],
            "special_case_cli_relocated_jetty": counts["special_case_cli_relocated_jetty"],
            "unmatched": counts["unmatched"],
            "metadata_only_component_count": sum(
                item["metadata_only_or_unmatched"] for item in dependency_components.values()
            ),
        },
        "components": sorted(dependency_components.values(), key=lambda item: item["coordinate"]),
        "class_attribution": class_results,
        "evidence_policy": {
            "exact_byte_match": "one dependency JAR contains the same class path and identical class bytes",
            "path_match_bytes_differ": "one dependency JAR contains the same class path but class bytes differ; transformation or build variance is possible",
            "ambiguous": "multiple dependency JARs contain the same class path; all candidates are retained",
            "unmatched": "no copied runtime dependency JAR contains the class path; may be first-party, generated, transformed, or otherwise unresolved",
            "cli_jetty": "relocated org/htmlunit/jetty classes are a dedicated special case; see shaded provenance report for pinned upstream module-prefix mapping",
            "metadata": "embedded Maven metadata is not treated as proof of executable code",
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", required=True, type=Path)
    parser.add_argument("--dependency-dir", required=True, type=Path)
    parser.add_argument("--distribution", required=True, choices=("cli", "ide"))
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    report = audit(args.archive, args.dependency_dir, args.distribution)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(
        f"{args.distribution}: {report['summary']['archive_class_count']} archive classes; "
        f"{report['summary']['unique_exact_byte_match']} unique exact matches; "
        f"{report['summary']['unique_path_match_bytes_differ']} path-only matches; "
        f"{report['summary']['unmatched']} unmatched"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
