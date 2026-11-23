#!/usr/bin/env bash
# Run all deterministic Phase 01 audit and CI tooling checks feasible without Android SDK/NDK/JDK.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

echo "=== 1. Phase 01 audit verification tests ==="
python3 tools/ci/phase01_audit_test.py

echo "=== 2. String catalog generator tests ==="
bash tools/build_strings-test.sh

echo "=== 3. 16 KB ELF alignment script tests ==="
bash tools/ci/check-so-alignment-test.sh

echo "=== 4. ARM64 BTI/PAC branch-protection script tests ==="
bash tools/ci/check-branch-protection-test.sh

echo "=== All Phase 01 local verification checks passed ==="
