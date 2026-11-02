#!/usr/bin/env bash
#
# Reads the R8 statistics embedded in the release AABs of the phone (:app) and
# automotive (:automotive) modules and prints them alongside the recorded
# pre-change baseline. This is the local measurement loop described in
# openspec/changes/improve-r8-optimization/design.md (D10): the numbers come
# from the shipped bundle's BUNDLE-METADATA/com.android.tools/r8.json, which is
# the same input Google Play's R8 recommendation uses.
#
# Usage:
#   scripts/r8-report.sh            # build the bundles, then report
#   scripts/r8-report.sh --no-build # report from the existing bundles
#
set -euo pipefail

cd "$(dirname "$0")/.."

BUILD=1
if [[ "${1:-}" == "--no-build" ]]; then
    BUILD=0
fi

if [[ "$BUILD" == "1" ]]; then
    echo "Building release bundles..."
    ./gradlew :app:bundleRelease :automotive:bundleRelease
fi

# Recorded pre-change values (Play "no-*" percentages from the baseline bundle).
APP_BASELINE_OBF=83.86
APP_BASELINE_OPT=83.95
APP_BASELINE_SHR=83.92
AUTOMOTIVE_BASELINE_OBF=89.33
AUTOMOTIVE_BASELINE_OPT=89.43
AUTOMOTIVE_BASELINE_SHR=89.39

report() {
    local label="$1"
    local aab="$2"
    local base_obf="$3"
    local base_opt="$4"
    local base_shr="$5"

    if [[ ! -f "$aab" ]]; then
        echo "$label: missing $aab (run without --no-build)" >&2
        return 1
    fi

    python3 - "$label" "$aab" "$base_obf" "$base_opt" "$base_shr" <<'PY'
import json
import sys
import zipfile

label, aab, base_obf, base_opt, base_shr = sys.argv[1:6]

with zipfile.ZipFile(aab) as z:
    raw = z.read("BUNDLE-METADATA/com.android.tools/r8.json")
data = json.loads(raw)
stats = data["stats"]

keys = [
    ("obfuscation", "noObfuscationPercentage", float(base_obf)),
    ("optimisation", "noOptimizationPercentage", float(base_opt)),
    ("shrinking", "noShrinkingPercentage", float(base_shr)),
]

print(f"\n=== {label} ===")
print(f"  bundle: {aab}")
for name, key, base in keys:
    no_pct = stats[key]
    rate = 100.0 - no_pct
    delta = base - no_pct
    sign = "+" if delta >= 0 else ""
    print(
        f"  {name:<13} applied {rate:6.2f}%  "
        f"(no-{name} {no_pct:6.2f}%, baseline {base:6.2f}%, delta {sign}{delta:.2f}pp)"
    )
print(
    "  optimized resource shrinking: "
    f"{data.get('resourceOptimization', {}).get('isOptimizedShrinkingEnabled')}"
)
PY
}

report "phone (:app)" "app/build/outputs/bundle/release/app-release.aab" \
    "$APP_BASELINE_OBF" "$APP_BASELINE_OPT" "$APP_BASELINE_SHR"

report "automotive (:automotive)" "automotive/build/outputs/bundle/release/automotive-release.aab" \
    "$AUTOMOTIVE_BASELINE_OBF" "$AUTOMOTIVE_BASELINE_OPT" "$AUTOMOTIVE_BASELINE_SHR"
