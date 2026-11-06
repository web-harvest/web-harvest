#!/usr/bin/env bash
# Build shared packages from a clean checkout. Used by pretest/pretype-check/prebuild
# so Maven frontend-maven-plugin and local npm scripts are self-sufficient.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
META="${ROOT}/packages/visual-shared/metadata-client"
XML="${ROOT}/packages/visual-shared/xml-roundtrip"

npm install --no-audit --no-fund --prefix "${META}"
npm run build --prefix "${META}"

npm install --no-audit --no-fund --prefix "${XML}"
npm run build --prefix "${XML}"
