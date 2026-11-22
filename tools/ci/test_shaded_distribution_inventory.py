#!/usr/bin/env python3
"""Regression tests for shaded distribution provenance inventory."""
from __future__ import annotations

import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).with_name("shaded_distribution_inventory.py")


class ShadedInventoryTests(unittest.TestCase):
    def make_archive(self, root: Path) -> Path:
        path = root / "sample.jar"
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("org/htmlunit/jetty/client/Client.class", b"class-bytes")
            jar.writestr("META-INF/maven/org.eclipse.jetty/jetty-client/pom.properties",
                         "groupId=org.eclipse.jetty\nartifactId=jetty-client\nversion=12.1.15\n")
            jar.writestr("META-INF/maven/org.eclipse.jetty/jetty-client/pom.xml",
                         "<project><licenses><license><name>EPL-2.0 OR Apache-2.0</name></license></licenses></project>")
            jar.writestr("META-INF/NOTICE.txt", "A third-party notice")
        return path

    def test_jetty_metadata_is_not_misrepresented_as_artifact_specific_code_mapping(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive = self.make_archive(root)
            output = root / "report.json"
            subprocess.run([
                sys.executable, str(SCRIPT), "--archive", str(archive),
                "--distribution", "cli", "--output", str(output),
            ], check=True, capture_output=True, text=True)
            report = json.loads(output.read_text(encoding="utf-8"))
            component = report["components"][0]
            self.assertEqual(component["purl"], "pkg:maven/org.eclipse.jetty/jetty-client@12.1.15?type=jar")
            self.assertEqual(component["code_attribution"], "namespace_correlated_not_artifact_specific")
            self.assertEqual(component["class_count_correlated_namespace"], 1)
            self.assertEqual(component["embedded_pom_licenses"], ["EPL-2.0 OR Apache-2.0"])
            self.assertEqual(report["class_inventory"]["org_htmlunit_jetty_classes"], 1)
            self.assertEqual(report["legal_resources"][0]["archive_entry"], "META-INF/NOTICE.txt")

    def test_metadata_only_coordinate_stays_unattributed(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive = self.make_archive(root)
            with zipfile.ZipFile(archive, "a") as jar:
                jar.writestr("META-INF/maven/example/metadata-only/pom.properties",
                             "groupId=example\nartifactId=metadata-only\nversion=1.0\n")
            output = root / "report.json"
            subprocess.run([
                sys.executable, str(SCRIPT), "--archive", str(archive),
                "--distribution", "cli", "--output", str(output),
            ], check=True, capture_output=True, text=True)
            report = json.loads(output.read_text(encoding="utf-8"))
            item = next(x for x in report["components"] if x["name"] == "metadata-only")
            self.assertEqual(item["code_attribution"], "embedded_metadata_only_unattributed")
            self.assertEqual(item["declared_licenses"], [])

    def test_aggregate_sbom_membership_is_explicit(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive = self.make_archive(root)
            sbom = root / "bom.json"
            sbom.write_text(json.dumps({"bomFormat": "CycloneDX", "components": [
                {"purl": "pkg:maven/org.eclipse.jetty/jetty-client@12.1.15?type=jar",
                 "licenses": [{"license": {"id": "EPL-2.0"}}]}
            ]}), encoding="utf-8")
            output = root / "report.json"
            subprocess.run([
                sys.executable, str(SCRIPT), "--archive", str(archive),
                "--distribution", "ide", "--sbom", str(sbom), "--output", str(output),
            ], check=True, capture_output=True, text=True)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(report["components"][0]["aggregate_sbom"], "present")
            self.assertEqual(report["components"][0]["aggregate_sbom_licenses"], ["EPL-2.0"])
            self.assertEqual(report["components"][0]["license_evidence"], "embedded_pom")


if __name__ == "__main__":
    unittest.main()
