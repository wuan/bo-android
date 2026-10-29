# Car support (Android Auto and Android Automotive OS)

Blitzortung runs as a **Weather-category templated car app** on both Android Auto
(projected from the phone) and Android Automotive OS (installed in the car). It
shows the live lightning map with the alert list beside it, reusing the phone
app's data, location and alert logic.

## What the car experience looks like

- **Map** – strikes drawn on an osmdroid map, coloured by age, with the current
  position marked. Pan and zoom with the map action strip (`Pan`, zoom out, zoom
  in).
- **Alert list** – the closest strike's distance and bearing, then one row per
  sector with its strike count.
- **States** – an empty state before data arrives and an error state when a fetch
  fails. The latest failed result is never shown as current.
- **Theme** – the map follows the host's light/dark theme.

Only the Weather category is supported (`androidx.car.app.category.WEATHER`) with
the `androidx.car.app.MAP_TEMPLATES` permission. Navigation and both map
permissions together are deliberately not declared.

## Module layout

| Module | Role |
| --- | --- |
| `:core` | Shared non-UI logic: `data`, `location`, `alert`, `protocol`, `preferences`, `util`, the map overlays, plus the Dagger `AppModule`/`ServiceModule`. |
| `:car` | Car experience: `CarAppService`, `Session`, `BlitzortungMapScreen`, `CarMapRenderer`, `CarAlertListPresenter`, `CarLocationPermission`, and the shared car manifest entries (service, category, permissions). |
| `:app` | Phone app. Adds the Android Auto declarations (`com.google.android.gms.car.application`, `automotive_app_desc.xml`) and `AppCarDependencies`. |
| `:automotive` | Android Automotive OS app (minSdk 29). Manifests the `CarAppActivity`, `android.hardware.type.automotive`, `android.software.car.templates_host` and `com.android.automotive`, and builds its own Dagger graph (`AutomotiveApplication` + `AutomotiveComponent`). |

The `CarAppService` is declared once in `:car` and merged into both applications.

The shared car manifest also declares the minimum Car App API level
(`androidx.car.app.minCarApiLevel = 7`, because `MapWithContentTemplate` was
introduced at level 7); the car host refuses to start an app that does not
declare it.

## How it works

- The hosting application registers its graph in `CarDependenciesProvider` during
  `Application.onCreate` (`AppCarDependencies` on the phone, `AutomotiveCarDependencies`
  in the car). This lets the framework-instantiated car service reach the shared
  `StrikeDataRepository`, `LocationRepository`, `AlertRepository` and
  `StrikeColorHandler` without depending on an application module.
- The car app never fetches data itself; it observes the same repositories the
  phone UI uses, so cache and update scheduling are shared.
- The map is drawn by the app: osmdroid's `MapView` (with the shared strike and
  own-location overlays) is hosted in a `Presentation` on a `VirtualDisplay`
  created from the surface the car host provides, and `SurfaceCallback`
  interactions are forwarded to the map controller.
- Location is requested through `CarContext.requestPermissions`. When access is
  denied the configured default/manual location remains the fallback.

## Testing

- Unit tests use `androidx.car.app:app-testing` (`TestCarContext`) and Robolectric.
- Android Auto: install the debug APK and use the **Desktop Head Unit (DHU)** to
  confirm discovery, pan/zoom and the theme toggle.
- Android Automotive OS: run the **AAOS emulator**, install
  `./gradlew :automotive:installDebug` and launch the app from the car launcher.
- Manifest invariants:
  ```bash
  ./gradlew :app:processDebugMainManifest :automotive:processDebugMainManifest
  python3 config/verify-car-manifests.py
  ```

## Releasing to cars

The phone and embedded apps share the application ID `org.blitzortung.android.app`
so they appear under one Google Play listing; the embedded artifact is published
on the **dedicated Android Automotive OS track**, not the mobile track.

1. **Build the embedded bundle**
   ```bash
   ./gradlew :automotive:bundleRelease
   ```
   The bundle is unsigned in this repository; sign it with the release key (or
   rely on Play App Signing) before uploading.
2. **Opt in to the form factors** in the Play Console under **Advanced settings →
   Form factors**: add **Android Auto** and **Android Automotive OS**, and agree
   to the Android Automotive OS review policy.
3. **Screenshots** – Android Automotive OS requires at least two portrait
   (800×1280) and two landscape (1024×786) screenshots of the current version,
   taken on a generic system image (the AAOS emulator's *Automotive* profiles).
4. **Track** – publish the `:automotive` artifact on the dedicated **Android
   Automotive OS only** track. The templated artifact must never be uploaded to
   the mobile track.
5. **Review** – submissions that contain a car artifact go through the car app
   quality guidelines review. Closed testing is non-blocking, open testing and
   production are blocking.

Because the car experience is a templated app, the phone UI is unaffected: the
existing activity, map, alerts, settings, widget and background monitoring behave
exactly as before.
