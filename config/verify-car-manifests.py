#!/usr/bin/env python3
"""Verify the Android Auto / Android Automotive OS car manifest invariants.

Checks the *merged* manifests of the phone app and the embedded app so the
rules that Google reviews against cannot regress silently:

  * exactly one androidx.car.app.category.* category, and it is WEATHER
  * androidx.car.app.MAP_TEMPLATES is present
  * androidx.car.app.NAVIGATION_TEMPLATES is absent (declaring both is rejected)
  * the CarAppService is exported
  * (embedded only) the CarAppActivity launcher entry point is exported

Run after building the merged manifests, e.g.:

    ./gradlew :app:processDebugMainManifest :automotive:processDebugMainManifest
    python3 config/verify-car-manifests.py
"""

from __future__ import annotations

import glob
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
WEATHER = "androidx.car.app.category.WEATHER"
MAP_TEMPLATES = "androidx.car.app.MAP_TEMPLATES"
NAVIGATION_TEMPLATES = "androidx.car.app.NAVIGATION_TEMPLATES"
ACCESS_SURFACE = "androidx.car.app.ACCESS_SURFACE"
MIN_CAR_API_LEVEL = "androidx.car.app.minCarApiLevel"
EXPECTED_MIN_CAR_API_LEVEL = "7"

MANIFESTS = {
    "app (Android Auto)": "app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
    "automotive (Android Automotive OS)": "automotive/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
}


def check(label: str, path_pattern: str, embedded: bool) -> list[str]:
    matches = glob.glob(path_pattern)
    if not matches:
        return [f"{label}: merged manifest not found at {path_pattern}"]
    root = ET.parse(matches[0]).getroot()

    failures: list[str] = []

    categories = {
        element.get(ANDROID + "name")
        for element in root.iter("category")
        if (element.get(ANDROID + "name") or "").startswith("androidx.car.app.category.")
    }
    if categories != {WEATHER}:
        failures.append(f"{label}: expected exactly the {WEATHER} category, found {sorted(categories)}")

    permissions = {element.get(ANDROID + "name") for element in root.iter("uses-permission")}
    if MAP_TEMPLATES not in permissions:
        failures.append(f"{label}: missing {MAP_TEMPLATES}")
    if ACCESS_SURFACE not in permissions:
        failures.append(f"{label}: missing {ACCESS_SURFACE}")
    if NAVIGATION_TEMPLATES in permissions:
        failures.append(f"{label}: {NAVIGATION_TEMPLATES} must not be declared")

    min_api_level = None
    for metadata in root.iter("meta-data"):
        if metadata.get(ANDROID + "name") == MIN_CAR_API_LEVEL:
            min_api_level = metadata.get(ANDROID + "value")
    if min_api_level != EXPECTED_MIN_CAR_API_LEVEL:
        failures.append(
            f"{label}: {MIN_CAR_API_LEVEL} must be {EXPECTED_MIN_CAR_API_LEVEL} (was {min_api_level})",
        )

    service_exported = None
    for service in root.iter("service"):
        if (service.get(ANDROID + "name") or "").endswith("BlitzortungCarAppService"):
            service_exported = service.get(ANDROID + "exported")
    if service_exported != "true":
        failures.append(f"{label}: BlitzortungCarAppService must be exported=true (was {service_exported})")

    if embedded:
        activity_exported = None
        for activity in root.iter("activity"):
            if (activity.get(ANDROID + "name") or "").endswith("CarAppActivity"):
                activity_exported = activity.get(ANDROID + "exported")
        if activity_exported != "true":
            failures.append(f"{label}: CarAppActivity must be exported=true (was {activity_exported})")

    if not failures:
        print(f"OK  {label}")
    return failures


def main() -> int:
    failures: list[str] = []
    for index, (label, path) in enumerate(MANIFESTS.items()):
        failures += check(label, path, embedded=index == 1)

    if failures:
        for failure in failures:
            print(f"FAIL  {failure}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
