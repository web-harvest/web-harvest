#!/usr/bin/env python3
"""Build per-distribution CycloneDX BOMs and aggregate-BOM delta reports.

Resolved dependency JAR hashes identify the source artifacts inspected by the
class-origin audit. They are not hashes of separately extracted components
inside the shaded archive. The root component hash always identifies the exact
shaded distribution archive.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import uuid


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def purl_for(group: str, name: str, version: str) -> str:
    return f"pkg:maven/{group}/{name}@{version}?type=jar"


def coordinate(group: str, name: str, version: str) -> str:
    return f"{group}:{name}:{version}:jar"


def load_json(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return value


def license_records(provenance: dict) -> tuple[list[dict], str]:
    embedded = provenance.get("embedded_pom_licenses") or []
    aggregate = provenance.get("aggregate_sbom_licenses") or []
    upstream = (provenance.get("upstream_project_license_evidence") or {}).get(
        "license_choices_declared_by_upstream", []
    )
    if embedded:
        return [{"license": {"name": value}} for value in embedded], "embedded_pom"
    if aggregate:
        # The inventory stores either SPDX IDs or human-readable names without
        # retaining the original field type. Preserve the value as a name rather
        # than incorrectly asserting that every string is a valid SPDX identifier.
        records = [{"license": {"name": value}} for value in aggregate
                   if isinstance(value, str) and value]
        if records:
            return records, "aggregate_sbom_effective_model"
    if upstream:
        expression = " OR ".join(upstream)
        return [{"expression": expression}], "pinned_upstream_project_model_choices"
    return [], "unresolved"


def build(provenance_path: Path, origin_path: Path, aggregate_path: Path,
          distribution: str, output_path: Path, delta_path: Path) -> tuple[dict, dict]:
    provenance = load_json(provenance_path)
    origin = load_json(origin_path)
    aggregate = load_json(aggregate_path)

    if provenance.get("distribution") != distribution or origin.get("distribution") != distribution:
        raise ValueError("Distribution labels do not match requested output")
    if provenance.get("archive", {}).get("sha256") != origin.get("archive", {}).get("sha256"):
        raise ValueError("Provenance and class-origin reports refer to different shaded archives")
    if aggregate.get("bomFormat") != "CycloneDX":
        raise ValueError("Aggregate input is not a CycloneDX BOM")

    archive_sha = provenance["archive"]["sha256"]
    if len(archive_sha) != 64:
        raise ValueError("Invalid shaded archive SHA-256")
    version = None
    for item in provenance.get("components", []):
        if item.get("group") == "org.webharvest" and item.get("name") in (
            "webharvest-cli", "webharvest-ide"
        ):
            version = item.get("version")
            break
    if not version:
        # The artifact version is also present in the distribution's embedded self PURL.
        self_component = next((c for c in origin.get("components", [])
                               if c.get("coordinate", "").startswith(
                                   f"org.webharvest:webharvest-{distribution}:")), None)
        if self_component:
            version = self_component["coordinate"].split(":")[2]
    if not version:
        raise ValueError("Cannot establish the shaded distribution version from embedded metadata")

    aggregate_components = {
        item["purl"]: item for item in aggregate.get("components", [])
        if isinstance(item, dict) and isinstance(item.get("purl"), str)
    }
    provenance_by_coordinate = {
        coordinate(item["group"], item["name"], item["version"]): item
        for item in provenance.get("components", [])
    }
    origin_by_coordinate = {
        item["coordinate"]: item for item in origin.get("components", [])
    }

    # The union preserves metadata-only coordinates and dependencies whose
    # executable classes were byte-correlated but whose POM metadata was not shaded.
    all_coordinates = sorted(set(provenance_by_coordinate) | set(origin_by_coordinate))
    components = []
    component_refs = []
    distribution_purls = set()
    for coord in all_coordinates:
        parts = coord.split(":")
        if len(parts) != 4:
            raise ValueError(f"Unsupported Maven coordinate: {coord}")
        group, name, component_version, packaging = parts
        if packaging != "jar":
            continue
        purl = purl_for(group, name, component_version)
        # The distribution itself is represented by metadata.component, not as
        # a duplicate library entry in metadata.component.dependencies.
        if group == "org.webharvest" and name == f"webharvest-{distribution}":
            continue
        distribution_purls.add(purl)
        p = provenance_by_coordinate.get(coord, {})
        o = origin_by_coordinate.get(coord, {})
        agg = aggregate_components.get(purl)
        licenses, license_source = license_records(p)

        hashes = []
        dependency_hashes = sorted(set(o.get("dependency_jar_sha256", [])))
        if len(dependency_hashes) == 1:
            hashes = [{"alg": "SHA-256", "content": dependency_hashes[0]}]

        props = [
            {"name": "webharvest:evidence:distribution-archive-sha256", "value": archive_sha},
            {"name": "webharvest:evidence:archive-pom-metadata", "value": str(bool(p.get("metadata_evidence"))).lower()},
            {"name": "webharvest:evidence:aggregate-sbom-purl-membership",
             "value": "present" if agg else "absent"},
            {"name": "webharvest:evidence:license-source", "value": license_source},
            {"name": "webharvest:evidence:class-origin-status",
             "value": "not_correlated" if not o else (
                 "metadata_only_or_unmatched" if o.get("metadata_only_or_unmatched")
                 else "byte_correlated_or_explicit_special_case"
             )},
            {"name": "webharvest:evidence:dependency-class-count",
             "value": str(o.get("dependency_class_count", 0))},
            {"name": "webharvest:evidence:matched-archive-class-count",
             "value": str(o.get("matched_archive_class_count", 0))},
            {"name": "webharvest:evidence:unmatched-dependency-class-count",
             "value": str(o.get("unmatched_dependency_class_count", 0))},
            {"name": "webharvest:evidence:dependency-jar-hash-semantics",
             "value": "SHA-256 of resolved source dependency JAR; not a hash of an extracted shaded component"},
        ]
        if p.get("code_attribution"):
            props.append({"name": "webharvest:evidence:namespace-attribution",
                          "value": p["code_attribution"]})
        if not agg:
            props.append({"name": "webharvest:evidence:aggregate-sbom-delta",
                          "value": "distribution_component_absent_from_aggregate_purl_set"})
        for index, value in enumerate(sorted(set(o.get("dependency_jars", [])))):
            props.append({"name": f"webharvest:evidence:dependency-jar-path:{index}", "value": value})
        for index, value in enumerate(dependency_hashes):
            props.append({"name": f"webharvest:evidence:dependency-jar-sha256:{index}", "value": value})

        item = {
            "type": "library",
            "bom-ref": purl,
            "group": group,
            "name": name,
            "version": component_version,
            "purl": purl,
            "scope": "required",
            "properties": props,
        }
        if hashes:
            item["hashes"] = hashes
        if licenses:
            item["licenses"] = licenses
        components.append(item)
        component_refs.append(purl)

    root_purl = purl_for("org.webharvest", f"webharvest-{distribution}", version)
    complete_distribution_purls = distribution_purls | {root_purl}
    aggregate_only = sorted(set(aggregate_components) - complete_distribution_purls)
    distribution_only = sorted(complete_distribution_purls - set(aggregate_components))
    root_ref = f"urn:webharvest:distribution:{distribution}:{version}:{archive_sha}"
    root_component = {
        "type": "application",
        "bom-ref": root_ref,
        "name": f"WebHarvest {distribution.upper()} distribution",
        "version": version,
        "purl": purl_for("org.webharvest", f"webharvest-{distribution}", version),
        "hashes": [{"alg": "SHA-256", "content": archive_sha}],
        "properties": [
            {"name": "webharvest:evidence:archive-file", "value": provenance["archive"]["file"]},
            {"name": "webharvest:evidence:archive-class-count",
             "value": str(origin["archive"]["class_count"])},
            {"name": "webharvest:evidence:aggregate-bom-sha256",
             "value": sha256_file(aggregate_path)},
            {"name": "webharvest:evidence:component-membership-policy",
             "value": "union of embedded Maven metadata coordinates and resolved runtime dependency coordinates; root distribution is metadata.component"},
            {"name": "webharvest:evidence:root-component-purl",
             "value": purl_for("org.webharvest", f"webharvest-{distribution}", version)},
        ],
    }
    bom = {
        "bomFormat": "CycloneDX",
        "specVersion": "1.6",
        "serialNumber": "urn:uuid:" + str(uuid.uuid5(uuid.NAMESPACE_URL, root_ref)),
        "version": 1,
        "metadata": {
            "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
            "component": root_component,
            "properties": [
                {"name": "webharvest:evidence:distribution", "value": distribution},
                {"name": "webharvest:evidence:aggregate-sbom-comparison",
                 "value": "PURL membership only; not proof of package-content equivalence"},
            ],
        },
        "components": components,
        "dependencies": [{"ref": root_ref, "dependsOn": sorted(component_refs)}]
            + [{"ref": ref, "dependsOn": []} for ref in sorted(component_refs)],
    }
    delta = {
        "schema": "webharvest.distribution-sbom-delta.v1",
        "distribution": distribution,
        "archive": provenance["archive"],
        "aggregate_sbom": {"file": aggregate_path.name, "sha256": sha256_file(aggregate_path)},
        "distribution_component_count": len(complete_distribution_purls),
        "aggregate_component_count": len(aggregate_components),
        "distribution_only_purls": distribution_only,
        "aggregate_only_purls": aggregate_only,
        "comparison_policy": "PURL membership only; not proof of package-content equivalence",
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    delta_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(bom, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    delta_path.write_text(json.dumps(delta, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return bom, delta


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--provenance", required=True, type=Path)
    parser.add_argument("--class-origin", required=True, type=Path)
    parser.add_argument("--aggregate-sbom", required=True, type=Path)
    parser.add_argument("--distribution", required=True, choices=("cli", "ide"))
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--delta-output", required=True, type=Path)
    args = parser.parse_args()
    bom, delta = build(args.provenance, args.class_origin, args.aggregate_sbom,
                       args.distribution, args.output, args.delta_output)
    print(f"{args.distribution}: {len(bom['components'])} distribution components; "
          f"{len(delta['distribution_only_purls'])} distribution-only PURLs; "
          f"{len(delta['aggregate_only_purls'])} aggregate-only PURLs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
