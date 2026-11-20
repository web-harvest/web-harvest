#!/usr/bin/env python3
"""Fail-closed validation for CycloneDX Maven SBOMs and OSV-Scanner results."""

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


def validate_scan(path: Path, exit_code: int) -> None:
    if exit_code not in (0, 1):
        raise ValueError(
            f"OSV-Scanner exited {exit_code}; only 0 (clean) or 1 (findings) "
            "can represent a completed scan"
        )
    result = load_json(path)
    results = result.get("results")
    if not isinstance(results, list) or not results:
        raise ValueError("OSV result JSON has no results array")
    package_count = 0
    vulnerability_count = 0
    for source in results:
        if not isinstance(source, dict):
            raise ValueError("OSV result contains a malformed source entry")
        packages = source.get("packages")
        if not isinstance(packages, list):
            raise ValueError("OSV result source has no packages array")
        package_count += len(packages)
        for package in packages:
            if not isinstance(package, dict) or not isinstance(package.get("package"), dict):
                raise ValueError("OSV result contains a malformed package entry")
            vulnerabilities = package.get("vulnerabilities", [])
            if not isinstance(vulnerabilities, list):
                raise ValueError("OSV package vulnerabilities must be an array")
            vulnerability_count += len(vulnerabilities)
    if package_count == 0:
        raise ValueError("OSV result contains no scanned packages")
    if exit_code == 0 and vulnerability_count:
        raise ValueError("OSV returned exit 0 but JSON contains vulnerabilities")
    if exit_code == 1 and not vulnerability_count:
        raise ValueError("OSV returned exit 1 without any vulnerability findings")
    print(
        f"Validated OSV scan: {package_count} packages, "
        f"{vulnerability_count} vulnerabilities, exit {exit_code}"
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    sbom = subparsers.add_parser("validate-sbom")
    sbom.add_argument("path", type=Path)
    scan = subparsers.add_parser("validate-scan")
    scan.add_argument("path", type=Path)
    scan.add_argument("--exit-code", required=True, type=int)
    args = parser.parse_args()
    try:
        if args.command == "validate-sbom":
            validate_sbom(args.path)
        else:
            validate_scan(args.path, args.exit_code)
    except ValueError as exc:
        print(f"::error::{exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
