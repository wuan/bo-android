# Tasks

## 1. Decouple the shared layer from the UI

- [x] 1.1 Move the log tag out of `Main` into a shared constant and replace every `Main.LOG_TAG` reference in `alert`, `data`, and `location` with it; verify `./gradlew testDebugUnitTest` passes
- [x] 1.2 Remove the `OwnMapView` dependency from `MainDataHandler` by inverting the grid/animation coupling (an interface or a view-agnostic callback) so the data layer no longer imports a `MapView` subclass; verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.data.*"` passes and the phone map/grid behaviour is unchanged on a device
- [x] 1.3 Remove the `app.controller.NotificationHandler` dependency from `AlertHandler` by inverting it behind an interface implemented in `:app`; verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.alert.*"` passes
- [x] 1.4 Relocate `PreferenceKey`, `OnSharedPreferenceChangeListener`, and the preference read/write helpers out of `app.view` into a shared package and update their references; verify `./gradlew testDebugUnitTest` passes
- [x] 1.5 Replace the remaining `Main` references in `data`, `alert`, and `location` with the minimal `Context`/port they actually need; verify `./gradlew testDebugUnitTest` passes

## 2. Extract the `:core` library module

- [ ] 2.1 Create an Android library module `:core` (namespace `org.blitzortung.android`, minSdk 23, Java 21, dependency locking) and register it in `settings.gradle.kts`; verify `./gradlew :core:assembleDebug` succeeds
- [x] 2.2 Move the `protocol` (including the new `MapViewport` and `AlertNotifier` ports) and `preferences` packages, plus `util` (sources and tests), into `:core`, moving the resources they use (`unit_km`, `unit_miles`) into `:core/res`, and add `implementation(project(":core"))` to `:app`; verify `./gradlew testDebugUnitTest` passes
- [x] 2.3 Move the `jsonrpc` and `data` packages and their tests into `:core`, moving the resources they use (`provider_warning`); verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.jsonrpc.*" --tests "org.blitzortung.android.data.*"` passes
- [x] 2.4 Move the `location` and `alert` packages and their tests into `:core`, moving the resources they use (`location_provider_disabled`, `direction_names`, `Green`, `RedWarn`, `Yellow`, the alert strings) across all locale `strings.xml` files; verify `./gradlew testDebugUnitTest` passes
- [x] 2.5 Move the Dagger modules that only wire the moved packages (`AppModule`, `ServiceModule`) into `:core`, keeping the activity/preference bindings and `PreferencesBackup` in `:app`; verify `./gradlew :app:assembleDebug` succeeds
- [x] 2.6 Regenerate the dependency lock files so `:core` is locked, and verify a clean `./gradlew testDebugUnitTest :app:assembleDebug`

## 3. Car module foundation (`:car`)

- [x] 3.1 Add the Car App Library version (`1.7.0`) and the `app`, `app-projected`, `app-automotive`, and `app-testing` libraries to `gradle/libs.versions.toml`; verify dependency resolution by regenerating lock files with `--write-locks`
- [x] 3.2 Create the Android library module `:car` (minSdk 23) depending on `:core` and `androidx.car.app:app`, with a manifest declaring the `CarAppService`, the `androidx.car.app.category.WEATHER` category, and the `MAP_TEMPLATES` and `ACCESS_SURFACE` permissions; verify the declared entries appear in the merged manifest via `./gradlew :car:processDebugMainManifest`
- [x] 3.3 Define the car dependency-provider interface that exposes the strike, location, and alert repositories plus alert parameters, and implement it in `:app` from `AppComponent`; verify a unit test resolves the repositories through the provider
- [x] 3.4 Implement the `CarAppService` (host validator, session creation) and a `Session` whose start screen returns a `MapWithContentTemplate`; add an `app-testing` unit test asserting the service creates a session and the start screen is a map-with-content screen
- [x] 3.5 Verify no car-specific data fetching is introduced by asserting (in a unit test or review check) that the session only observes the injected repositories and never constructs a data provider

## 4. In-car map rendering

- [x] 4.1 Define a renderer interface for the car map surface and implement the osmdroid-backed renderer that hosts a `MapView` (with strike and own-location overlays) in a `Presentation` on a `VirtualDisplay` created from the host `Surface`, per the design; verify a unit test covers the lat/lon-to-screen projection and the renderer compiles into `:car`
- [x] 4.2 Reuse the existing strike age coloring and overlays for the car map (strikes colored by age, own position marked, expired strikes dropped); verify a unit test asserts the strike-to-color mapping matches the phone behaviour
- [x] 4.3 Implement the `SurfaceCallback` (surface lifecycle plus `onScroll`, `onScale`, `onFling`, `onClick` forwarded to the `MapView` controller) and a map action strip containing `Action.PAN` and zoom actions; verify a unit test asserts the action strip contains `PAN` and a manual check on the Desktop Head Unit pans and zooms
- [x] 4.4 Support the host light/dark theme by redrawing with dark styling when `CarContext.isDarkMode` is true and on `Session.onCarConfigurationChanged`; verify manually on the Desktop Head Unit that toggling the car theme redraws the map

## 5. Car alert list, empty and error states, and location

- [ ] 5.1 Render the alert list as the `MapWithContentTemplate` content template from `AlertRepository` state: closest strike distance and bearing plus per-sector counts; verify an `app-testing` unit test asserts the rows for a sample `AlertResult`
- [ ] 5.2 Render an empty state when no strike data exists and an error state when a fetch fails without presenting the failed result as current; verify unit tests for both states
- [ ] 5.3 Request location via `CarContext.requestPermissions` and feed the result into the existing `LocationRepository`, falling back to the configured manual/default location on denial; verify a unit test asserts the denial path does not crash and yields the fallback location

## 6. Android Auto wiring in `:app`

- [ ] 6.1 Add `implementation(project(":car"))` and `androidx.car.app:app-projected` to `:app`, add `res/xml/automotive_app_desc.xml` containing `<uses name="template"/>`, and declare the `com.google.android.gms.car.application` metadata; verify the merged `:app` manifest contains the metadata and descriptor via `./gradlew :app:processDebugMainManifest`
- [ ] 6.2 Register the car dependency provider during `:app` startup; verify the Android Auto host discovers and launches the car app on the Desktop Head Unit (`adb install` the debug APK, then start it from the DHU car launcher)

## 7. Android Automotive OS module (`:automotive`)

- [ ] 7.1 Create the application module `:automotive` (minSdk 29, same application id as `:app`) depending on `:core` and `:car` plus `androidx.car.app:app-automotive`, with the AAOS manifest (`android.hardware.type.automotive`, `android.software.car.templates_host`, `com.android.automotive` metadata and `automotive_app_desc.xml`, and the `CarAppActivity` entry point with `distractionOptimized`); verify `./gradlew :automotive:assembleDebug` succeeds and the merged manifest declares the AAOS features
- [ ] 7.2 Add the automotive application/Dagger component on top of the `:core` modules and register the car dependency provider; verify `./gradlew :automotive:testDebugUnitTest` passes with a test that the session resolves its repositories
- [ ] 7.3 Install on an Android Automotive OS emulator and verify the car app launches from the launcher and shows the same map and alert-list screens as Android Auto

## 8. Car assets and metadata

- [ ] 8.1 Add the car launcher icon and any car-specific strings/theme used by `:car`/`:automotive`; verify the resources resolve and `./gradlew :automotive:assembleDebug` includes them
- [ ] 8.2 Confirm the manifests declare exactly one car category (`WEATHER`) and never both `MAP_TEMPLATES` and `NAVIGATION_TEMPLATES`, and that activities are correctly `exported`; verify with a manifest check on the built artifacts

## 9. Distribution and documentation

- [ ] 9.1 Document the car experience and the release process (form-factor opt-in, car screenshots, dedicated Android Automotive OS track, car app quality review) in the project docs; verify the documented steps match the Play Console screens they reference
- [ ] 9.2 Sign a release bundle for `:automotive` and confirm it is targeted at the dedicated Android Automotive OS track (not the mobile track); verify by uploading to an internal testing track and checking the track/form-factor assignment

## 10. Integration verification

- [ ] 10.1 Run `./gradlew testDebugUnitTest` across all modules and verify every new and existing unit test passes
- [ ] 10.2 Run `./gradlew lint` and verify no new errors are introduced by the new modules
- [ ] 10.3 Verify the phone experience is unchanged: the phone launcher, map, alerts, settings, widget, and background monitoring behave as before and `./gradlew :app:assembleRelease` still produces a valid phone artifact
- [ ] 10.4 Verify the car experience is consistent across platforms on the Desktop Head Unit (Android Auto) and the AAOS emulator: same start screen, map with strikes, and alert list

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and update the `car-integration` spec as needed.
