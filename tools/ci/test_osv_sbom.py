import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("osv_sbom.py")


def run_validator(*args):
    return subprocess.run(
        [sys.executable, str(SCRIPT), *map(str, args)],
        capture_output=True,
        text=True,
        check=False,
    )


class SbomValidationTests(unittest.TestCase):
    def write_json(self, directory, name, value):
        path = directory / name
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    def valid_sbom(self):
        return {
            "bomFormat": "CycloneDX",
            "specVersion": "1.6",
            "metadata": {"component": {"name": "webharvest", "type": "application", "bom-ref": "pkg:maven/example/root@1.0"}},
            "components": [
                {"type": "library", "name": "example", "version": "1.0", "purl": "pkg:maven/example/example@1.0", "bom-ref": "pkg:maven/example/example@1.0"}
            ],
            "dependencies": [
                {"ref": "pkg:maven/example/root@1.0", "dependsOn": ["pkg:maven/example/example@1.0"]},
                {"ref": "pkg:maven/example/example@1.0", "dependsOn": []}
            ],
        }

    def test_accepts_complete_maven_sbom(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_json(Path(temp), "bom.json", self.valid_sbom())
            self.assertEqual(run_validator("validate-sbom", path).returncode, 0)

    def test_rejects_empty_components(self):
        with tempfile.TemporaryDirectory() as temp:
            bom = self.valid_sbom()
            bom["components"] = []
            path = self.write_json(Path(temp), "bom.json", bom)
            self.assertNotEqual(run_validator("validate-sbom", path).returncode, 0)

    def test_rejects_missing_package_urls(self):
        with tempfile.TemporaryDirectory() as temp:
            bom = self.valid_sbom()
            bom["components"][0].pop("purl")
            path = self.write_json(Path(temp), "bom.json", bom)
            self.assertNotEqual(run_validator("validate-sbom", path).returncode, 0)

    def test_rejects_missing_dependency_graph(self):
        with tempfile.TemporaryDirectory() as temp:
            bom = self.valid_sbom()
            bom["dependencies"] = []
            path = self.write_json(Path(temp), "bom.json", bom)
            self.assertNotEqual(run_validator("validate-sbom", path).returncode, 0)

    def test_rejects_component_without_purl_even_when_other_components_have_one(self):
        with tempfile.TemporaryDirectory() as temp:
            bom = self.valid_sbom()
            bom["components"].append({"type": "library", "name": "missing-purl"})
            path = self.write_json(Path(temp), "bom.json", bom)
            self.assertNotEqual(run_validator("validate-sbom", path).returncode, 0)

    def test_rejects_dangling_dependency_reference(self):
        with tempfile.TemporaryDirectory() as temp:
            bom = self.valid_sbom()
            bom["dependencies"][0]["dependsOn"] = ["pkg:maven/missing/missing@1.0"]
            path = self.write_json(Path(temp), "bom.json", bom)
            self.assertNotEqual(run_validator("validate-sbom", path).returncode, 0)

    def test_rejects_missing_component_dependency_node(self):
        with tempfile.TemporaryDirectory() as temp:
            bom = self.valid_sbom()
            bom["dependencies"] = []
            path = self.write_json(Path(temp), "bom.json", bom)
            self.assertNotEqual(run_validator("validate-sbom", path).returncode, 0)


class ScanResultValidationTests(unittest.TestCase):
    def write_sbom(self, directory, purls=None):
        purls = purls or ["pkg:maven/example/example@1.0"]
        path = directory / "bom.json"
        path.write_text(
            json.dumps({
                "bomFormat": "CycloneDX",
                "specVersion": "1.6",
                "components": [{"purl": purl} for purl in purls],
            }),
            encoding="utf-8",
        )
        return path

    def write_result(self, directory, vulnerabilities):
        path = directory / "scan.json"
        path.write_text(
            json.dumps({
                "results": [{
                    "source": {"type": "sbom", "path": "bom.json"},
                    "packages": [{
                        "purl": "pkg:maven/example/example@1.0",
                        "package": {"name": "example", "version": "1.0", "ecosystem": "Maven"},
                        "vulnerabilities": vulnerabilities,
                    }],
                }]
            }),
            encoding="utf-8",
        )
        return path

    def test_accepts_clean_scan(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_result(Path(temp), [])
            sbom = self.write_sbom(Path(temp))
            self.assertEqual(run_validator("validate-scan", path, "--exit-code", 0, "--sbom", sbom).returncode, 0)

    def test_accepts_scan_with_findings(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_result(Path(temp), [{"id": "GHSA-example"}])
            sbom = self.write_sbom(Path(temp))
            self.assertEqual(run_validator("validate-scan", path, "--exit-code", 1, "--sbom", sbom).returncode, 0)

    def test_rejects_failure_without_findings(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_result(Path(temp), [])
            sbom = self.write_sbom(Path(temp))
            self.assertNotEqual(run_validator("validate-scan", path, "--exit-code", 1, "--sbom", sbom).returncode, 0)

    def test_rejects_scanner_execution_failure_even_with_json(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_result(Path(temp), [])
            sbom = self.write_sbom(Path(temp))
            self.assertNotEqual(run_validator("validate-scan", path, "--exit-code", 127, "--sbom", sbom).returncode, 0)

    def test_rejects_missing_scan_output(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "missing.json"
            sbom = self.write_sbom(Path(temp))
            self.assertNotEqual(run_validator("validate-scan", path, "--exit-code", 0, "--sbom", sbom).returncode, 0)

    def test_rejects_partial_scan_json(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "partial.json"
            path.write_text('{"results":[', encoding="utf-8")
            sbom = self.write_sbom(Path(temp))
            self.assertNotEqual(run_validator("validate-scan", path, "--exit-code", 0, "--sbom", sbom).returncode, 0)

    def test_rejects_missing_packages(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "scan.json"
            path.write_text(json.dumps({"results": [{"packages": []}]}), encoding="utf-8")
            sbom = self.write_sbom(Path(temp))
            self.assertNotEqual(run_validator("validate-scan", path, "--exit-code", 0, "--sbom", sbom).returncode, 0)

    def test_rejects_missing_package_url(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_result(Path(temp), [])
            result = json.loads(path.read_text(encoding="utf-8"))
            result["results"][0]["packages"][0].pop("purl")
            path.write_text(json.dumps(result), encoding="utf-8")
            self.assertNotEqual(run_validator("validate-scan", path, "--exit-code", 0).returncode, 0)


    def test_rejects_incomplete_scan_inventory(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            path = self.write_result(directory, [])
            sbom = self.write_sbom(directory, [
                "pkg:maven/example/example@1.0",
                "pkg:maven/example/second@2.0",
            ])
            self.assertNotEqual(
                run_validator("validate-scan", path, "--exit-code", 0, "--sbom", sbom).returncode,
                0,
            )

    def test_rejects_unexpected_scan_package(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            path = self.write_result(directory, [])
            sbom = self.write_sbom(directory, ["pkg:maven/example/other@9.9"])
            self.assertNotEqual(
                run_validator("validate-scan", path, "--exit-code", 0, "--sbom", sbom).returncode,
                0,
            )

    def test_compare_detects_new_package_advisory_pair(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            base = self.write_result(directory, [])
            head = directory / "head.json"
            head.write_text(
                json.dumps({
                    "results": [{
                        "source": {"type": "sbom", "path": "bom.json"},
                        "packages": [{
                            "purl": "pkg:maven/example/example@1.0",
                            "package": {"name": "example", "version": "1.0", "ecosystem": "Maven"},
                            "vulnerabilities": [{"id": "GHSA-new"}],
                        }],
                    }]
                }),
                encoding="utf-8",
            )
            output = directory / "new-findings.json"
            self.assertEqual(run_validator("compare", base, head, "--output", output).returncode, 1)
            self.assertIn("GHSA-new", output.read_text(encoding="utf-8"))

    def test_compare_accepts_unchanged_clean_inventory(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            base = self.write_result(directory, [])
            head = directory / "head.json"
            head.write_text(base.read_text(encoding="utf-8"), encoding="utf-8")
            output = directory / "new-findings.json"
            self.assertEqual(run_validator("compare", base, head, "--output", output).returncode, 0)


if __name__ == "__main__":
    unittest.main()
