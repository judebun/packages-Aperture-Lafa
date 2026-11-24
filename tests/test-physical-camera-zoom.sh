#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 judebun
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
test_output="$(mktemp -d)"
trap 'rm -rf -- "$test_output"' EXIT

"${KOTLINC_BIN:-kotlinc}" \
    "$repo_root/app/src/main/java/org/lineageos/aperture/utils/PhysicalCameraZoom.kt" \
    "$repo_root/tests/PhysicalCameraZoomTest.kt" \
    -jvm-target 17 -include-runtime -d "$test_output/physical-camera-zoom-tests.jar"

java -jar "$test_output/physical-camera-zoom-tests.jar"
