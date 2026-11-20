#!/usr/bin/env python3
"""Fail-closed validation and base/head comparison for OSV SBOM scans."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys


def load_json(path: Path) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"cannot read valid JSON from {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return value


def validate_sbom(path: Path) -> None:
    bom = load_json(path)
    if bom.get("bomFormat") != "CycloneDX":
        raise ValueError("SBOM bomFormat must be CycloneDX")
    if not str(bom.get("specVersion", "")).startswith("1."):
        raise ValueError("SBOM specVersion is missing or unsupported")
    metadata = bom.get("metadata")
    if not isinstance(metadata, dict) or not isinstance(metadata.get("component"), dict):
        raise ValueError("SBOM metadata.component is missing")
    components = bom.get("components")
    if not isinstance(components, list) or not components:
        raise ValueError("SBOM components must be a non-empty array")
    purls = [
        component.get("purl")
        for component in components
        if isinstance(component, dict) and isinstance(component.get("purl"), str)
    ]
    if not purls:
        raise ValueError("SBOM contains no Package URLs")
    if not any(purl.startswith("pkg:maven/") for purl in purls):
        raise ValueError("SBOM contains no Maven Package URL")
    dependencies = bom.get("dependencies")
    if not isinstance(dependencies, list) or not dependencies:
        raise ValueError("SBOM dependency graph is missing or empty")
    print(
        f"Validated CycloneDX {bom['specVersion']} SBOM: "
        f"{len(components)} components, {len(purls)} Package URLs, "
        f"{len(dependencies)} dependency nodes"
    )


def scan_findings(path: Path) -> tuple[int, set[tuple[str, str]]]:
    result = load_json(path)
    results = result.get("results")
    if not isinstance(results, list) or not results:
        raise ValueError("OSV result JSON has no results array")
    package_count = 0
    findings: set[tuple[str, str]] = set()
    for source in results:
        if not isinstance(source, dict):
            raise ValueError("OSV result contains a malformed source entry")
        packages = source.get("packages")
        if not isinstance(packages, list):
            raise ValueError("OSV result source has no packages array")
        package_count += len(packages)
        for item in packages:
            if not isinstance(item, dict) or not isinstance(item.get("package"), dict):
                raise ValueError("OSV result contains a malformed package entry")
            package = item["package"]
            purl = item.get("purl") or package.get("purl")
            if not isinstance(purl, str) or not purl.startswith("pkg:"):
                raise ValueError("OSV package result is missing its Package URL")
            vulnerabilities = item.get("vulnerabilities", [])
            if not isinstance(vulnerabilities, list):
                raise ValueError("OSV package vulnerabilities must be an array")
            for vulnerability in vulnerabilities:
                if not isinstance(vulnerability, dict) or not isinstance(vulnerability.get("id"), str):
                    raise ValueError("OSV result contains a vulnerability without an ID")
                findings.add((purl, vulnerability["id"]))
    if package_count == 0:
        raise ValueError("OSV result contains no scanned packages")
    return package_count, findings


def validate_scan(path: Path, exit_code: int) -> None:
    if exit_code not in (0, 1):
        raise ValueError(
            f"OSV-Scanner exited {exit_code}; only 0 (clean) or 1 (findings) "
            "can represent a completed scan"
        )
    package_count, findings = scan_findings(path)
    if exit_code == 0 and findings:
        raise ValueError("OSV returned exit 0 but JSON contains vulnerabilities")
    if exit_code == 1 and not findings:
        raise ValueError("OSV returned exit 1 without any vulnerability findings")
    print(
        f"Validated OSV scan: {package_count} packages, "
        f"{len(findings)} package/advisory findings, exit {exit_code}"
    )


def compare_scans(base_path: Path, head_path: Path, output_path: Path) -> int:
    base_count, base_findings = scan_findings(base_path)
    head_count, head_findings = scan_findings(head_path)
    introduced = sorted(head_findings - base_findings)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(
            {
                "base_packages": base_count,
                "head_packages": head_count,
                "new_findings": [
                    {"purl": purl, "id": advisory_id}
                    for purl, advisory_id in introduced
                ],
            },
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    print(
        f"Compared base ({base_count} packages) with head ({head_count} packages): "
        f"{len(introduced)} new package/advisory findings"
    )
    for purl, advisory_id in introduced:
        print(f"::error title=New OSV finding::{advisory_id} affects {purl}")
    return 1 if introduced else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    sbom = subparsers.add_parser("validate-sbom")
    sbom.add_argument("path", type=Path)
    scan = subparsers.add_parser("validate-scan")
    scan.add_argument("path", type=Path)
    scan.add_argument("--exit-code", required=True, type=int)
    compare = subparsers.add_parser("compare")
    compare.add_argument("base", type=Path)
    compare.add_argument("head", type=Path)
    compare.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        if args.command == "validate-sbom":
            validate_sbom(args.path)
            return 0
        if args.command == "validate-scan":
            validate_scan(args.path, args.exit_code)
            return 0
        return compare_scans(args.base, args.head, args.output)
    except ValueError as exc:
        print(f"::error::{exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
