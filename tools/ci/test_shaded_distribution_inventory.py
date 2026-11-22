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
            self.assertEqual(component["code_attribution"], "class_prefix_correlated_to_upstream_module_source")
            self.assertEqual(component["class_evidence"]["class_count"], 1)
            self.assertEqual(component["class_count_correlated_namespace"], 1)
            self.assertEqual(component["class_evidence"]["upstream_module"], "jetty-core/jetty-client")
            self.assertEqual(component["class_evidence"]["upstream_commit"], "770f5e4d1c39673d6356163118a1e6bf1117ced3")
            self.assertEqual(report["class_inventory"]["jetty_class_attribution"]["mapped_class_count"], 1)
            self.assertEqual(component["embedded_pom_licenses"], ["EPL-2.0 OR Apache-2.0"])
            self.assertEqual(report["class_inventory"]["org_htmlunit_jetty_classes"], 1)
            self.assertEqual(report["legal_resources"][0]["archive_entry"], "META-INF/NOTICE.txt")
            self.assertEqual(report["jetty_notice_audit"]["content_level_reconciliation"], "open")
            self.assertEqual(report["jetty_notice_audit"]["archive_named_jetty_notice_candidates"], [])

    def test_ide_jetty_modules_are_mapped_without_websocket_overlap(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive = root / "ide.jar"
            modules = [
                ("org.eclipse.jetty", "jetty-server", "org/eclipse/jetty/server/Server.class"),
                ("org.eclipse.jetty", "jetty-security", "org/eclipse/jetty/security/SecurityHandler.class"),
                ("org.eclipse.jetty", "jetty-session", "org/eclipse/jetty/session/Session.class"),
                ("org.eclipse.jetty", "jetty-xml", "org/eclipse/jetty/xml/Xml.class"),
                ("org.eclipse.jetty.websocket", "jetty-websocket-core-common", "org/eclipse/jetty/websocket/core/Frame.class"),
                ("org.eclipse.jetty.websocket", "jetty-websocket-core-server", "org/eclipse/jetty/websocket/core/server/Handshaker.class"),
                ("org.eclipse.jetty.ee9", "jetty-ee9-nested", "org/eclipse/jetty/ee9/nested/Handler.class"),
                ("org.eclipse.jetty.ee9", "jetty-ee9-security", "org/eclipse/jetty/ee9/security/Security.class"),
                ("org.eclipse.jetty.ee9", "jetty-ee9-servlet", "org/eclipse/jetty/ee9/servlet/Servlet.class"),
                ("org.eclipse.jetty.ee9", "jetty-ee9-webapp", "org/eclipse/jetty/ee9/webapp/WebApp.class"),
                ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-api", "org/eclipse/jetty/ee9/websocket/api/Session.class"),
                ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-common", "org/eclipse/jetty/ee9/websocket/common/Frame.class"),
                ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-server", "org/eclipse/jetty/ee9/websocket/server/Creator.class"),
                ("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-servlet", "org/eclipse/jetty/ee9/websocket/servlet/Servlet.class"),
                ("org.eclipse.jetty.ee", "jetty-ee-webapp", "org/eclipse/jetty/ee/webapp/ClassLoader.class"),
            ]
            with zipfile.ZipFile(archive, "w") as jar:
                for group, artifact, class_path in modules:
                    jar.writestr(class_path, b"class-bytes")
                    base = f"META-INF/maven/{group}/{artifact}/"
                    jar.writestr(base + "pom.properties",
                                 f"groupId={group}\nartifactId={artifact}\nversion=12.1.15\n")
                jar.writestr("META-INF/NOTICE.txt", "Unrelated notice")
            output = root / "report.json"
            subprocess.run([
                sys.executable, str(SCRIPT), "--archive", str(archive),
                "--distribution", "ide", "--output", str(output),
            ], check=True, capture_output=True, text=True)
            report = json.loads(output.read_text(encoding="utf-8"))
            attribution = report["class_inventory"]["jetty_class_attribution"]
            self.assertEqual(attribution["mapped_class_count"], len(modules))
            self.assertEqual(attribution["unmapped_class_count"], 0)
            components = {item["name"]: item for item in report["components"]}
            self.assertEqual(
                sum(item["class_count_correlated_namespace"] for item in components.values()),
                len(modules),
            )
            self.assertEqual(components["jetty-websocket-core-common"]["class_evidence"]["class_count"], 1)
            self.assertEqual(components["jetty-websocket-core-server"]["class_evidence"]["class_count"], 1)
            self.assertEqual(components["jetty-websocket-core-common"]["class_evidence"]["excluded_package_prefixes"],
                             ["websocket/core/client/", "websocket/core/server/"])

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
            self.assertEqual(item["embedded_pom_licenses"], [])


    def test_cli_jetty_license_falls_back_to_pinned_upstream_project_model(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive = root / "jetty-cli.jar"
            with zipfile.ZipFile(archive, "w") as jar:
                jar.writestr("org/htmlunit/jetty/client/Client.class", b"class-bytes")
                jar.writestr("META-INF/maven/org.eclipse.jetty/jetty-client/pom.properties",
                             "groupId=org.eclipse.jetty\nartifactId=jetty-client\nversion=12.1.15\n")
                jar.writestr("META-INF/maven/org.eclipse.jetty/jetty-client/pom.xml", "<project/>")
                jar.writestr("META-INF/NOTICE.txt", "Unrelated third-party notice")
            output = root / "report.json"
            subprocess.run([
                sys.executable, str(SCRIPT), "--archive", str(archive),
                "--distribution", "cli", "--output", str(output),
            ], check=True, capture_output=True, text=True)
            report = json.loads(output.read_text(encoding="utf-8"))
            component = report["components"][0]
            evidence = component["upstream_project_license_evidence"]
            self.assertEqual(component["license_evidence"], "upstream_project_root_license_and_notice")
            self.assertEqual(evidence["license_choices_declared_by_upstream"], ["EPL-2.0", "Apache-2.0"])
            self.assertEqual(evidence["effective_license_model_pom_git_blob_sha"],
                             "e4d1540441e9cce523e166681a67edb190765367")
            self.assertEqual(evidence["notice_file_git_blob_sha"],
                             "ea13d34f3c2e9a22eb2b5c9fd1919f7f6b9d61db")

    def test_aggregate_sbom_membership_is_explicit(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive = self.make_archive(root)
            with zipfile.ZipFile(archive, "a") as jar:
                jar.writestr("META-INF/maven/example/inherited-license/pom.properties",
                             "groupId=example\nartifactId=inherited-license\nversion=1.0\n")
                jar.writestr("META-INF/maven/example/inherited-license/pom.xml", "<project/>")
            sbom = root / "bom.json"
            sbom.write_text(json.dumps({"bomFormat": "CycloneDX", "components": [
                {"purl": "pkg:maven/org.eclipse.jetty/jetty-client@12.1.15?type=jar",
                 "licenses": [{"license": {"id": "EPL-2.0"}}]},
                {"purl": "pkg:maven/example/inherited-license@1.0?type=jar",
                 "licenses": [{"license": {"id": "Apache-2.0"}}]}
            ]}), encoding="utf-8")
            output = root / "report.json"
            subprocess.run([
                sys.executable, str(SCRIPT), "--archive", str(archive),
                "--distribution", "ide", "--sbom", str(sbom), "--output", str(output),
            ], check=True, capture_output=True, text=True)
            report = json.loads(output.read_text(encoding="utf-8"))
            jetty = next(x for x in report["components"] if x["name"] == "jetty-client")
            self.assertEqual(jetty["aggregate_sbom"], "present")
            self.assertEqual(jetty["aggregate_sbom_licenses"], ["EPL-2.0"])
            self.assertEqual(jetty["license_evidence"], "embedded_pom")
            inherited = next(x for x in report["components"] if x["name"] == "inherited-license")
            self.assertEqual(inherited["aggregate_sbom"], "present")
            self.assertEqual(inherited["aggregate_sbom_licenses"], ["Apache-2.0"])
            self.assertEqual(inherited["license_evidence"], "aggregate_sbom_effective_model")


if __name__ == "__main__":
    unittest.main()
