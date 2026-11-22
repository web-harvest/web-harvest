#!/usr/bin/env python3
"""Regression tests for exact shaded-class origin correlation."""
from __future__ import annotations

import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).with_name("shaded_class_origin_audit.py")


def write_jar(path: Path, entries: dict[str, bytes]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as jar:
        for name, payload in sorted(entries.items()):
            jar.writestr(name, payload)


def maven_properties(group: str, artifact: str, version: str) -> bytes:
    return (
        f"groupId={group}\nartifactId={artifact}\nversion={version}\n"
    ).encode("ascii")


class ShadedClassOriginAuditTests(unittest.TestCase):
    def run_audit(self, root: Path, distribution: str = "ide") -> dict:
        archive = root / "distribution.jar"
        output = root / "report.json"
        subprocess.run([
            sys.executable, str(SCRIPT),
            "--archive", str(archive),
            "--dependency-dir", str(root / "dependencies"),
            "--distribution", distribution,
            "--output", str(output),
        ], check=True, capture_output=True, text=True)
        return json.loads(output.read_text(encoding="utf-8"))

    def test_exact_byte_match_and_metadata_only_coordinate_are_distinct(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            archive_entries = {
                "com/example/Exact.class": b"same-class-bytes",
                "META-INF/maven/example/exact/pom.properties":
                    maven_properties("example", "exact", "1.0"),
                "META-INF/maven/example/metadata-only/pom.properties":
                    maven_properties("example", "metadata-only", "1.0"),
            }
            write_jar(root / "distribution.jar", archive_entries)
            write_jar(root / "dependencies/example/exact/1.0/exact-1.0.jar", {
                "META-INF/maven/example/exact/pom.properties":
                    maven_properties("example", "exact", "1.0"),
                "com/example/Exact.class": b"same-class-bytes",
            })
            report = self.run_audit(root)
            self.assertEqual(report["summary"]["unique_exact_byte_match"], 1)
            self.assertEqual(report["summary"]["unmatched"], 0)
            exact = next(x for x in report["components"] if x["coordinate"] == "example:exact:1.0:jar")
            self.assertEqual(exact["unique_exact_byte_match_count"], 1)
            metadata = next(x for x in report["components"] if x["coordinate"] == "example:metadata-only:1.0:jar")
            self.assertTrue(metadata["metadata_only_or_unmatched"])

    def test_unique_path_with_different_bytes_is_not_an_exact_match(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_jar(root / "distribution.jar", {"com/example/Changed.class": b"shaded-bytes"})
            write_jar(root / "dependencies/example/library/1.0/library-1.0.jar", {
                "com/example/Changed.class": b"upstream-bytes",
            })
            report = self.run_audit(root)
            self.assertEqual(report["summary"]["unique_path_match_bytes_differ"], 1)
            self.assertEqual(report["summary"]["unique_exact_byte_match"], 0)
            result = report["class_attribution"][0]
            self.assertEqual(result["status"], "unique_path_match_bytes_differ")

    def test_duplicate_class_paths_remain_ambiguous(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_jar(root / "distribution.jar", {"com/example/Duplicate.class": b"archive"})
            write_jar(root / "dependencies/example/one/1.0/one-1.0.jar", {
                "com/example/Duplicate.class": b"candidate-one",
            })
            write_jar(root / "dependencies/example/two/1.0/two-1.0.jar", {
                "com/example/Duplicate.class": b"candidate-two",
            })
            report = self.run_audit(root)
            self.assertEqual(report["summary"]["ambiguous_path_match_bytes_differ"], 1)
            self.assertEqual(len(report["class_attribution"][0]["candidates"]), 2)

    def test_cli_relocated_jetty_is_reported_as_a_special_case(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_jar(root / "distribution.jar", {
                "org/htmlunit/jetty/client/Client.class": b"relocated",
                "META-INF/maven/org.eclipse.jetty/jetty-client/pom.properties":
                    maven_properties("org.eclipse.jetty", "jetty-client", "12.1.15"),
            })
            write_jar(root / "dependencies/org/eclipse/jetty/jetty-client/12.1.15/jetty-client-12.1.15.jar", {
                "org/eclipse/jetty/client/Client.class": b"upstream",
            })
            report = self.run_audit(root, "cli")
            self.assertEqual(report["summary"]["special_case_cli_relocated_jetty"], 1)
            self.assertEqual(report["summary"]["unmatched"], 0)
            jetty = next(x for x in report["components"] if x["coordinate"].startswith("org.eclipse.jetty:"))
            self.assertFalse(jetty["metadata_only_or_unmatched"])

    def test_project_owned_class_without_dependency_candidate_is_first_party(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_jar(root / "distribution.jar", {
                "org/webharvest/server/WebHarvestServer.class": b"first-party",
            })
            write_jar(root / "dependencies/example/library/1.0/library-1.0.jar", {
                "com/example/Library.class": b"library",
            })
            report = self.run_audit(root)
            self.assertEqual(report["summary"]["first_party"], 1)
            self.assertEqual(report["summary"]["unmatched"], 0)
            self.assertEqual(report["class_attribution"][0]["status"], "first_party")

    def test_class_without_candidate_outside_project_namespace_is_unmatched(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_jar(root / "distribution.jar", {"generated/Generated.class": b"generated"})
            write_jar(root / "dependencies/example/library/1.0/library-1.0.jar", {
                "com/example/Library.class": b"library",
            })
            report = self.run_audit(root)
            self.assertEqual(report["summary"]["unmatched"], 1)
            self.assertEqual(report["class_attribution"][0]["status"], "unmatched")


if __name__ == "__main__":
    unittest.main()
