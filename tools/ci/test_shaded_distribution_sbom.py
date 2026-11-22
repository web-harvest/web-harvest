#!/usr/bin/env python3
"""Regression tests for distribution-specific CycloneDX generation."""
from __future__ import annotations

import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).parent))
from shaded_distribution_sbom import build


class DistributionSbomTests(unittest.TestCase):
    def test_union_preserves_metadata_and_byte_correlated_components(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            provenance = {
                "distribution": "cli",
                "archive": {"file": "webharvest-cli.jar", "sha256": "a" * 64},
                "legal_resources": [
                    {"archive_entry": "META-INF/LICENSE", "sha256": "f" * 64, "size_bytes": 128},
                    {"archive_entry": "META-INF/NOTICE", "sha256": "1" * 64, "size_bytes": 64},
                ],
                "jetty_notice_audit": {
                    "content_level_reconciliation": "open",
                    "upstream_notice_source": "https://example.invalid/jetty/NOTICE.txt",
                    "upstream_notice_git_blob_sha": "2" * 40,
                },
                "components": [
                    {
                        "group": "example", "name": "metadata-only", "version": "1.0",
                        "purl": "pkg:maven/example/metadata-only@1.0?type=jar",
                        "metadata_evidence": ["META-INF/maven/example/metadata-only/pom.properties"],
                        "embedded_pom_licenses": ["Apache-2.0"],
                        "aggregate_sbom_licenses": ["Apache-2.0"],
                    },
                    {
                        "group": "example", "name": "classified", "version": "1.0",
                        "purl": "pkg:maven/example/classified@1.0?type=jar",
                        "metadata_evidence": ["META-INF/maven/example/classified/pom.properties"],
                        "embedded_pom_licenses": ["BSD-3-Clause"],
                        "aggregate_sbom_licenses": [],
                    }
                ],
            }
            origin = {
                "distribution": "cli",
                "archive": {"file": "webharvest-cli.jar", "sha256": "a" * 64, "class_count": 2},
                "components": [
                    {
                        "coordinate": "example:metadata-only:1.0:jar",
                        "dependency_jar_sha256": ["b" * 64],
                        "dependency_jars": ["example/metadata-only/1.0/metadata-only-1.0.jar"],
                        "dependency_class_count": 0,
                        "matched_archive_class_count": 0,
                        "unmatched_dependency_class_count": 0,
                        "metadata_only_or_unmatched": True,
                    },
                    {
                        "coordinate": "example:byte-match:2.0:jar",
                        "dependency_jar_sha256": ["c" * 64],
                        "dependency_jars": ["example/byte-match/2.0/byte-match-2.0.jar"],
                        "dependency_class_count": 1,
                        "matched_archive_class_count": 1,
                        "unmatched_dependency_class_count": 0,
                        "metadata_only_or_unmatched": False,
                        "resolved_maven_pom_license_evidence": [{
                            "dependency_jar": "example/byte-match/2.0/byte-match-2.0.jar",
                            "dependency_jar_sha256": "c" * 64,
                            "pom_path": "example/byte-match/2.0/byte-match-2.0.pom",
                            "pom_sha256": "d" * 64,
                            "licenses": ["MIT"],
                            "evidence_scope": "direct license declarations in the resolved Maven repository POM; parent inheritance is not inferred",
                        }],
                    },
                    {
                        "coordinate": "example:classified:1.0:dom:jar",
                        "dependency_jar_sha256": ["e" * 64],
                        "dependency_jars": ["example/classified/1.0/classified-1.0-dom.jar"],
                        "dependency_class_count": 0,
                        "matched_archive_class_count": 0,
                        "unmatched_dependency_class_count": 0,
                        "metadata_only_or_unmatched": True,
                    },
                    {
                        "coordinate": "org.webharvest:webharvest-cli:2.3.1:jar",
                        "dependency_jar_sha256": [],
                        "dependency_jars": [],
                        "dependency_class_count": 0,
                        "matched_archive_class_count": 0,
                        "unmatched_dependency_class_count": 0,
                        "metadata_only_or_unmatched": True,
                    },
                ],
            }
            aggregate = {
                "bomFormat": "CycloneDX",
                "components": [
                    {"purl": "pkg:maven/example/metadata-only@1.0?type=jar", "scope": "required"},
                    {"purl": "pkg:maven/example/classified@1.0?classifier=dom&type=jar", "scope": "optional"},
                    {"purl": "pkg:maven/org.webharvest/webharvest-cli@2.3.1?type=jar"},
                    {"purl": "pkg:maven/example/aggregate-only@3.0?type=jar"},
                ],
            }
            p = root / "provenance.json"
            o = root / "origin.json"
            a = root / "aggregate.json"
            out = root / "distribution-bom.json"
            delta_out = root / "delta.json"
            p.write_text(json.dumps(provenance), encoding="utf-8")
            o.write_text(json.dumps(origin), encoding="utf-8")
            a.write_text(json.dumps(aggregate), encoding="utf-8")

            bom, delta = build(p, o, a, "cli", out, delta_out)
            by_name = {item["name"]: item for item in bom["components"]}
            self.assertEqual(set(by_name), {"metadata-only", "byte-match", "classified"})
            self.assertEqual(by_name["metadata-only"]["hashes"][0]["content"], "b" * 64)
            self.assertEqual(by_name["metadata-only"]["licenses"], [
                {"license": {"name": "Apache-2.0"}}
            ])
            self.assertEqual(by_name["byte-match"]["hashes"][0]["content"], "c" * 64)
            self.assertEqual(by_name["byte-match"]["licenses"], [{"license": {"name": "MIT"}}])
            self.assertEqual(
                next(x["value"] for x in by_name["byte-match"]["properties"]
                     if x["name"].endswith("license-source")),
                "resolved_maven_repository_pom_direct_declaration",
            )
            self.assertIn(
                "example/byte-match/2.0/byte-match-2.0.pom",
                [x["value"] for x in by_name["byte-match"]["properties"]
                 if x["name"].endswith("resolved-maven-pom-source:0")],
            )
            self.assertEqual(by_name["classified"]["purl"],
                             "pkg:maven/example/classified@1.0?classifier=dom&type=jar")
            self.assertEqual(by_name["classified"]["scope"], "optional")
            self.assertEqual(by_name["classified"]["licenses"], [{"license": {"name": "BSD-3-Clause"}}])
            self.assertEqual(
                next(x["value"] for x in by_name["classified"]["properties"]
                     if x["name"].endswith("license-source")),
                "shaded_embedded_pom",
            )
            self.assertNotIn("scope", by_name["byte-match"])
            self.assertIn(
                "SHA-256 of resolved source dependency JAR",
                next(x["value"] for x in by_name["byte-match"]["properties"]
                     if x["name"].endswith("dependency-jar-hash-semantics"))
            )
            self.assertEqual(delta["distribution_only_purls"], [
                "pkg:maven/example/byte-match@2.0?type=jar",
            ])
            self.assertEqual(delta["aggregate_only_purls"], [
                "pkg:maven/example/aggregate-only@3.0?type=jar"
            ])
            self.assertEqual(delta["aggregate_only_classifications"][0]["classification"],
                "aggregate_reactor_component_not_observed_in_distribution_embedded_metadata_or_runtime_dependency_inventory")
            self.assertEqual(
                next(x["classification"] for x in delta["distribution_only_classifications"]
                     if x["purl"] == "pkg:maven/example/byte-match@2.0?type=jar"),
                "runtime_dependency_class_byte_correlated",
            )
            self.assertEqual(bom["metadata"]["component"]["hashes"][0]["content"], "a" * 64)
            self.assertEqual(bom["metadata"]["component"]["purl"],
                             "pkg:maven/org.webharvest/webharvest-cli@2.3.1?type=jar")
            root_properties = {
                item["name"]: item["value"]
                for item in bom["metadata"]["component"]["properties"]
            }
            self.assertEqual(root_properties["webharvest:evidence:legal-resource-count"], "2")
            self.assertEqual(root_properties["webharvest:evidence:legal-resource:0:archive-entry"],
                             "META-INF/LICENSE")
            self.assertEqual(root_properties["webharvest:evidence:legal-resource:0:sha256"], "f" * 64)
            self.assertEqual(root_properties["webharvest:evidence:jetty-notice-content-reconciliation"],
                             "open")
            self.assertEqual(root_properties["webharvest:evidence:jetty-upstream-notice-git-blob-sha"],
                             "2" * 40)
            self.assertEqual(delta["legal_resources"][1]["archive_entry"], "META-INF/NOTICE")
            self.assertEqual(delta["jetty_notice_audit"]["content_level_reconciliation"], "open")
            self.assertTrue(out.exists())
            self.assertTrue(delta_out.exists())

    def test_refuses_archive_hash_mismatch(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            p = root / "p.json"
            o = root / "o.json"
            a = root / "a.json"
            p.write_text(json.dumps({
                "distribution": "ide", "archive": {"sha256": "a" * 64}, "components": []
            }), encoding="utf-8")
            o.write_text(json.dumps({
                "distribution": "ide", "archive": {"sha256": "b" * 64}, "components": []
            }), encoding="utf-8")
            a.write_text(json.dumps({"bomFormat": "CycloneDX", "components": []}), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "different shaded archives"):
                build(p, o, a, "ide", root / "out.json", root / "delta.json")


if __name__ == "__main__":
    unittest.main()
