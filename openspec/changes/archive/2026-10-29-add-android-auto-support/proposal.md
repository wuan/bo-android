## Why

The app is a phone-only experience: it has no Car App Library dependency, no
`CarAppService`, and no car-related manifest declarations, so it never appears on
Android Auto head units and cannot be installed on Android Automotive OS (AAOS)
cars. Drivers who rely on Blitzortung for live storm awareness have to glance at
their phone, which is unsafe and in many cars impossible while driving. Adding a
driver-optimized in-car experience brings the existing real-time lightning data to
the car screen through a supported category.

## What Changes

- Add a shared car module (`:car`) containing a `CarAppService`, `Session`, and
  template-based `Screen`s, reusing the existing data/alert repositories rather
  than re-fetching data in a car-specific way.
- Declare the app as a **Weather** category templated car app
  (`androidx.car.app.category.WEATHER`) with the `androidx.car.app.MAP_TEMPLATES`
  permission.
- Present a map (host-provided surface drawn by the app) with lightning strikes
  plus an alert list showing nearest-strike distance/direction and sector counts
  (`MapWithContentTemplate`).
- Declare Android Auto support in the existing phone app manifest
  (`com.google.android.gms.car.application` → `automotive_app_desc.xml` with
  `<uses name="template"/>`).
- Add a separate `:automotive` module for AAOS containing the AAOS manifest
  (`android.hardware.type.automotive`, `android.software.car.templates_host`,
  `com.android.automotive`) and the `CarAppActivity` entry point.
- Add car app assets (car launcher icon), unit tests for the session/screen state
  mapping via `androidx.car.app:app-testing`, and refresh the Gradle dependency
  lock files.
- **BREAKING (distribution only)**: the AAOS artifact must be published on the
  dedicated Android Automotive OS release track; it cannot share the mobile track
  with the templated experience.

## Capabilities

### New Capabilities
- `car-integration`: the app runs as a Weather-category templated car app on
  Android Auto and Android Automotive OS, projecting live lightning data and
  proximity alerts onto the car screen through a map surface and an alert list,
  while remaining the same installable mobile app on phones.

### Modified Capabilities
<!-- No existing capability's requirements change: the car app consumes the
     existing repositories/handlers without altering their contracts. -->

## Impact

- **Build/packaging**: new `:car` (shared, library) and `:automotive` (application)
  modules; new `androidx.car.app:app`, `:app-projected`, `:app-automotive`, and
  `:app-testing` dependencies; updated `settings.gradle.kts`,
  `gradle/libs.versions.toml`, and lock files.
- **Phone app**: `app/src/main/AndroidManifest.xml` gains the
  `com.google.android.gms.car.application` metadata and the `CarAppService`
  declaration; new `res/xml/automotive_app_desc.xml` and car icon resources.
- **New code**: car service/session/screens, a map `SurfaceCallback` renderer that
  reuses `StrikeColorHandler`-style age coloring, and an alert-list presenter
  driven by the existing `AlertRepository`/`MainViewModel` data.
- **Distribution**: Google Play form-factor opt-in (Android Auto and/or Android
  Automotive OS), dedicated AAOS track, car screenshots, and car app quality
  review.
- **Not affected**: the phone UI, data providers, alert computation, preference
  keys, and background service behavior.
