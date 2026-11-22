#!/usr/bin/env python3
"""Create an auditable provenance inventory for a shaded WebHarvest distribution.

Embedded Maven metadata is evidence of packaged metadata, not by itself proof that
classes from that component are present. The report deliberately keeps these
evidence levels separate and records unresolved attribution instead of guessing.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import zipfile
import xml.etree.ElementTree as ET


JETTY_RELOCATED_MODULES = {
    ("org.eclipse.jetty", "jetty-client"): {
        "source_module": "jetty-core/jetty-client",
        "package_prefixes": ["client/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-alpn-client"): {
        "source_module": "jetty-core/jetty-alpn/jetty-alpn-client",
        "package_prefixes": ["alpn/client/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-http"): {
        "source_module": "jetty-core/jetty-http",
        "package_prefixes": ["http/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-util"): {
        "source_module": "jetty-core/jetty-util",
        "package_prefixes": ["util/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-io"): {
        "source_module": "jetty-core/jetty-io",
        "package_prefixes": ["io/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.compression", "jetty-compression-gzip"): {
        "source_module": "jetty-core/jetty-compression/jetty-compression-gzip",
        "package_prefixes": ["compression/gzip/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.compression", "jetty-compression-common"): {
        "source_module": "jetty-core/jetty-compression/jetty-compression-common",
        "package_prefixes": ["compression/"],
        "exclude_prefixes": ["compression/gzip/"],
    },
    ("org.eclipse.jetty.websocket", "jetty-websocket-core-client"): {
        "source_module": "jetty-core/jetty-websocket/jetty-websocket-core-client",
        "package_prefixes": ["websocket/core/client/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.websocket", "jetty-websocket-core-common"): {
        "source_module": "jetty-core/jetty-websocket/jetty-websocket-core-common",
        "package_prefixes": ["websocket/core/"],
        "exclude_prefixes": ["websocket/core/client/", "websocket/core/server/"],
    },
    ("org.eclipse.jetty.websocket", "jetty-websocket-core-server"): {
        "source_module": "jetty-core/jetty-websocket/jetty-websocket-core-server",
        "package_prefixes": ["websocket/core/server/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-server"): {
        "source_module": "jetty-core/jetty-server",
        "package_prefixes": ["server/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-security"): {
        "source_module": "jetty-core/jetty-security",
        "package_prefixes": ["security/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-session"): {
        "source_module": "jetty-core/jetty-session",
        "package_prefixes": ["session/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty", "jetty-xml"): {
        "source_module": "jetty-core/jetty-xml",
        "package_prefixes": ["xml/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee", "jetty-ee-webapp"): {
        "source_module": "jetty-core/jetty-ee/jetty-ee-webapp",
        "package_prefixes": ["ee/webapp/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9", "jetty-ee9-nested"): {
        "source_module": "jetty-ee9/jetty-ee9-nested",
        "package_prefixes": ["ee9/nested/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9", "jetty-ee9-security"): {
        "source_module": "jetty-ee9/jetty-ee9-security",
        "package_prefixes": ["ee9/security/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9", "jetty-ee9-servlet"): {
        "source_module": "jetty-ee9/jetty-ee9-servlet",
        "package_prefixes": ["ee9/servlet/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9", "jetty-ee9-webapp"): {
        "source_module": "jetty-ee9/jetty-ee9-webapp",
        "package_prefixes": ["ee9/webapp/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-api"): {
        "source_module": "jetty-ee9/jetty-ee9-websocket/jetty-ee9-websocket-jetty-api",
        "package_prefixes": ["ee9/websocket/api/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-common"): {
        "source_module": "jetty-ee9/jetty-ee9-websocket/jetty-ee9-websocket-jetty-common",
        "package_prefixes": ["ee9/websocket/common/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-server"): {
        "source_module": "jetty-ee9/jetty-ee9-websocket/jetty-ee9-websocket-jetty-server",
        "package_prefixes": ["ee9/websocket/server/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-servlet"): {
        "source_module": "jetty-ee9/jetty-ee9-websocket/jetty-ee9-websocket-servlet",
        "package_prefixes": ["ee9/websocket/servlet/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.websocket", "jetty-websocket-jetty-api"): {
        "source_module": "jetty-core/jetty-websocket/jetty-websocket-jetty-api",
        "package_prefixes": ["websocket/api/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.websocket", "jetty-websocket-jetty-common"): {
        "source_module": "jetty-core/jetty-websocket/jetty-websocket-jetty-common",
        "package_prefixes": ["websocket/common/"],
        "exclude_prefixes": [],
    },
    ("org.eclipse.jetty.websocket", "jetty-websocket-jetty-client"): {
        "source_module": "jetty-core/jetty-websocket/jetty-websocket-jetty-client",
        "package_prefixes": ["websocket/client/"],
        "exclude_prefixes": [],
    },
}
JETTY_SOURCE_TAG = "jetty-12.1.15"
JETTY_SOURCE_COMMIT = "770f5e4d1c39673d6356163118a1e6bf1117ced3"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child_text(node: ET.Element, name: str) -> str | None:
    for child in node:
        if local_name(child.tag) == name and child.text:
            return child.text.strip()
    return None


def parse_embedded_pom(data: bytes) -> list[str]:
    try:
        root = ET.fromstring(data)
    except ET.ParseError:
        return []
    licenses = []
    for node in root.iter():
        if local_name(node.tag) == "license":
            name = next((child.text.strip() for child in node
                         if local_name(child.tag) == "name" and child.text), None)
            url = next((child.text.strip() for child in node
                        if local_name(child.tag) == "url" and child.text), None)
            value = name or url
            if value and value not in licenses:
                licenses.append(value)
    return licenses


def build_inventory(archive_path: Path, distribution: str, sbom_path: Path | None) -> dict:
    archive_bytes = archive_path.read_bytes()
    components: dict[tuple[str, str, str], dict] = {}
    class_entries: list[str] = []
    legal_resources = []
    with zipfile.ZipFile(archive_path) as archive:
        names = set(archive.namelist())
        for name in sorted(names):
            if name.endswith(".class") and not name.endswith("module-info.class"):
                class_entries.append(name)
            base = PurePosixPath(name).name.lower()
            if (base.startswith(("license", "notice", "copying", "copyright", "third-party"))
                    or base == "dependencies" or name.lower().startswith("meta-inf/licenses/")):
                payload = archive.read(name)
                legal_resources.append({
                    "archive_entry": name,
                    "size_bytes": len(payload),
                    "sha256": sha256(payload),
                })
            if not name.startswith("META-INF/maven/") or not name.endswith("/pom.properties"):
                continue
            raw = archive.read(name).decode("ISO-8859-1")
            props = {}
            for line in raw.splitlines():
                line = line.strip()
                if not line or line.startswith(("#", "!")) or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                props[key.strip()] = value.strip()
            group = props.get("groupId")
            artifact = props.get("artifactId")
            version = props.get("version")
            if not (group and artifact and version):
                continue
            key = (group, artifact, version)
            item = components.setdefault(key, {
                "group": group, "name": artifact, "version": version,
                "purl": f"pkg:maven/{group}/{artifact}@{version}?type=jar",
                "metadata_evidence": [],
                "embedded_pom": None,
                "embedded_pom_licenses": [],
                "aggregate_sbom_licenses": [],
                "upstream_project_license_evidence": None,
                "license_evidence": "unresolved",
                "code_attribution": "embedded_metadata_only_unattributed",
                "class_count_correlated_namespace": 0,
            })
            item["metadata_evidence"].append(name)
            pom_path = str(PurePosixPath(name).parent / "pom.xml")
            if pom_path in names:
                pom_data = archive.read(pom_path)
                item["embedded_pom"] = {
                    "archive_entry": pom_path,
                    "sha256": sha256(pom_data),
                    "size_bytes": len(pom_data),
                }
                item["embedded_pom_licenses"] = parse_embedded_pom(pom_data)

    jetty_class_entries = [name for name in class_entries if name.startswith("org/htmlunit/jetty/")]
    original_jetty_class_entries = [name for name in class_entries if name.startswith("org/eclipse/jetty/")]
    jetty_namespace = "org/htmlunit/jetty/" if jetty_class_entries else "org/eclipse/jetty/"
    attributed_classes: dict[str, str] = {}
    for item in components.values():
        spec = JETTY_RELOCATED_MODULES.get((item["group"], item["name"]))
        if not spec or item["version"] != JETTY_SOURCE_TAG.removeprefix("jetty-"):
            continue
        matched = [
            name for name in class_entries
            if any(name.startswith(jetty_namespace + prefix) for prefix in spec["package_prefixes"])
            and not any(name.startswith(jetty_namespace + prefix) for prefix in spec["exclude_prefixes"])
        ]
        duplicates = [name for name in matched if name in attributed_classes]
        if duplicates:
            raise ValueError(f"Jetty class package mapping is ambiguous: {duplicates[:5]}")
        for name in matched:
            attributed_classes[name] = item["purl"]
        item["code_attribution"] = (
            "class_prefix_correlated_to_upstream_module_source"
            if matched else "embedded_metadata_without_matching_class_prefix"
        )
        item["class_evidence"] = {
            "namespace_prefix": jetty_namespace,
            "package_prefixes": spec["package_prefixes"],
            "excluded_package_prefixes": spec["exclude_prefixes"],
            "class_count": len(matched),
            "class_entries": matched,
            "upstream_tag": JETTY_SOURCE_TAG,
            "upstream_commit": JETTY_SOURCE_COMMIT,
            "upstream_module": spec["source_module"],
            "upstream_pom": f"https://github.com/jetty/jetty.project/blob/{JETTY_SOURCE_TAG}/{spec['source_module']}/pom.xml",
        }

    aggregate_components = {}
    if sbom_path:
        bom = json.loads(sbom_path.read_text(encoding="utf-8"))
        if bom.get("bomFormat") != "CycloneDX":
            raise ValueError("aggregate SBOM is not CycloneDX")
        aggregate_components = {
            item.get("purl"): item for item in bom.get("components", [])
            if isinstance(item, dict) and isinstance(item.get("purl"), str)
        }

    for item in components.values():
        aggregate_component = aggregate_components.get(item["purl"])
        item["aggregate_sbom"] = (
            "present" if aggregate_component else
            "absent" if sbom_path else "not_compared"
        )
        if aggregate_component:
            item["aggregate_sbom_licenses"] = [
                license_item.get("license", {}).get("id")
                or license_item.get("license", {}).get("name")
                for license_item in aggregate_component.get("licenses", [])
                if isinstance(license_item, dict) and isinstance(license_item.get("license"), dict)
                and (license_item.get("license", {}).get("id") or license_item.get("license", {}).get("name"))
            ]
        if (item["group"], item["name"]) in JETTY_RELOCATED_MODULES and item["version"] == "12.1.15":
            item["upstream_project_license_evidence"] = {
                "project": "Eclipse Jetty",
                "source_tag": JETTY_SOURCE_TAG,
                "source_commit": JETTY_SOURCE_COMMIT,
                "license_choices_declared_by_upstream": ["EPL-2.0", "Apache-2.0"],
                "effective_license_model_pom": f"https://github.com/jetty/jetty.project/blob/{JETTY_SOURCE_TAG}/pom.xml",
                "effective_license_model_pom_git_blob_sha": "e4d1540441e9cce523e166681a67edb190765367",
                "license_file": f"https://github.com/jetty/jetty.project/blob/{JETTY_SOURCE_TAG}/LICENSE",
                "license_file_git_blob_sha": "22840e2944ffc53b1953091fc31954e69126348d",
                "notice_file": f"https://github.com/jetty/jetty.project/blob/{JETTY_SOURCE_TAG}/NOTICE.txt",
                "notice_file_git_blob_sha": "ea13d34f3c2e9a22eb2b5c9fd1919f7f6b9d61db",
            }
        if item["embedded_pom_licenses"]:
            item["license_evidence"] = "embedded_pom"
        elif item["aggregate_sbom_licenses"]:
            item["license_evidence"] = "aggregate_sbom_effective_model"
        elif item["upstream_project_license_evidence"]:
            item["license_evidence"] = "upstream_project_root_license_and_notice"

    roots: dict[str, int] = {}
    for name in class_entries:
        parts = name.split("/")
        root = "/".join(parts[:2]) if len(parts) > 2 else parts[0]
        roots[root] = roots.get(root, 0) + 1

    if not components:
        raise ValueError(f"{archive_path} contains no embedded Maven pom.properties coordinates")
    if not class_entries:
        raise ValueError(f"{archive_path} contains no class files")
    if not legal_resources:
        raise ValueError(f"{archive_path} contains no license/NOTICE-like resources")

    return {
        "schema": "webharvest.shaded-provenance.v1",
        "distribution": distribution,
        "archive": {
            "file": archive_path.name,
            "size_bytes": len(archive_bytes),
            "sha256": sha256(archive_bytes),
        },
        "evidence_policy": {
            "pom_properties": "metadata presence only; not proof of executable code",
            "code_attribution": "only explicitly correlated namespaces are marked; all other origins remain unattributed",
            "license": "embedded POM declarations take precedence; aggregate CycloneDX effective-model license data is recorded separately; otherwise unresolved",
        },
        "class_inventory": {
            "total_class_files": len(class_entries),
            "org_htmlunit_jetty_classes": len(jetty_class_entries),
            "org_eclipse_jetty_classes": len(original_jetty_class_entries),
            "org_htmlunit_jetty_class_entries": jetty_class_entries,
            "org_eclipse_jetty_class_entries": original_jetty_class_entries,
            "jetty_class_attribution": {
                "namespace": jetty_namespace,
                "mapped_class_count": len(attributed_classes),
                "unmapped_class_count": len(set(jetty_class_entries if jetty_class_entries else original_jetty_class_entries) - set(attributed_classes)),
            },
            "top_level_package_roots": dict(sorted(roots.items())),
        },
        "aggregate_sbom": {
            "file": sbom_path.name if sbom_path else None,
            "sha256": sha256(sbom_path.read_bytes()) if sbom_path else None,
            "comparison": "purl membership only; not proof of package-content equivalence",
        },
        "components": sorted(components.values(), key=lambda item: item["purl"]),
        "legal_resources": sorted(legal_resources, key=lambda item: item["archive_entry"]),
        "jetty_notice_audit": {
            "upstream_notice_source": f"https://github.com/jetty/jetty.project/blob/{JETTY_SOURCE_TAG}/NOTICE.txt",
            "upstream_notice_git_blob_sha": "ea13d34f3c2e9a22eb2b5c9fd1919f7f6b9d61db",
            "archive_notice_candidates": [
                item["archive_entry"] for item in legal_resources
                if "notice" in item["archive_entry"].lower()
            ],
            "archive_named_jetty_notice_candidates": [
                item["archive_entry"] for item in legal_resources
                if "notice" in item["archive_entry"].lower()
                and "jetty" in item["archive_entry"].lower()
            ],
            "content_level_reconciliation": "open",
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", required=True, type=Path)
    parser.add_argument("--distribution", required=True, choices=("cli", "ide"))
    parser.add_argument("--sbom", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    report = build_inventory(args.archive, args.distribution, args.sbom)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"{args.distribution}: {len(report['components'])} embedded coordinates; "
          f"{report['class_inventory']['total_class_files']} classes; "
          f"{len(report['legal_resources'])} legal resources; "
          f"archive SHA-256 {report['archive']['sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
