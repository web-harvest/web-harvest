#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.resolve(__dirname, '..');
const baselinePath = path.join(frontendRoot, 'jest-known-failures.json');
const logPath = process.argv[2];

if (!logPath) {
  console.error('Usage: assert-jest-gate.mjs <jest-output-file>');
  process.exit(2);
}

const log = fs.readFileSync(logPath, 'utf8');
const baseline = JSON.parse(fs.readFileSync(baselinePath, 'utf8'));

if (/SyntaxError: Unexpected token 'export'|d3\.mjs|d3\/src\/index\.js/.test(log)) {
  console.error('D3 ESM parse failure still present');
  process.exit(1);
}

const suites = log.match(/Test Suites:\s+(\d+) failed,\s+(\d+) passed,\s+(\d+) total/);
const tests = log.match(/Tests:\s+(\d+) failed,\s+(\d+) passed,\s+(\d+) total/);
if (!suites || !tests) {
  console.error('Could not parse Jest summary from output');
  console.error(log.slice(-2000));
  process.exit(1);
}

const failedSuites = Number(suites[1]);
const totalSuites = Number(suites[3]);
const failedTests = Number(tests[1]);
const totalTests = Number(tests[3]);

if (totalSuites !== baseline.requiredTotalSuites) {
  console.error(
    `Suite total drift: got ${totalSuites}, expected ${baseline.requiredTotalSuites}`
  );
  process.exit(1);
}

if (totalTests !== baseline.requiredTotalTests) {
  console.error(
    `Test total drift: got ${totalTests}, expected ${baseline.requiredTotalTests}`
  );
  process.exit(1);
}

if (failedSuites > baseline.maxFailedSuites) {
  console.error(
    `Failed suites ${failedSuites} exceed baseline ceiling ${baseline.maxFailedSuites}`
  );
  process.exit(1);
}

if (failedTests > baseline.maxFailedTests) {
  console.error(
    `Failed tests ${failedTests} exceed baseline ceiling ${baseline.maxFailedTests}`
  );
  process.exit(1);
}

console.log(
  `Jest gate OK: suites ${failedSuites}/${totalSuites} failed, tests ${failedTests}/${totalTests} failed (ceilings ${baseline.maxFailedSuites}/${baseline.maxFailedTests}).`
);
