# Design

## Context

See `proposal.md` — Why. The relevant current state:

- `:app` is a single Android application module (minSdk 23, target/compileSdk 37)
  containing everything: UI (`app`, `map`, `dialogs`, `widget`), the data layer
  (`data`, `jsonrpc`), `location`, `alert`, `protocol`, `util`, `settings`, plus
  the Dagger graph (`dagger`, `BOApplication`, `AppComponent`).
- The repository/ViewModel layer (`StrikeDataRepository`, `AlertRepository`,
  `LocationRepository`, `MainViewModel`) decouples UI from the
  `ConsumerContainer`-based handlers, so a second presentation surface can consume
  the same state.
- osmdroid (`org.osmdroid:osmdroid-android`) supplies tiles, `MapView`, overlays
  (`StrikeListOverlay`, `OwnLocationOverlay`, `StrikeColorHandler`).
- Android Auto runs the car app **in the phone app's process** (projected); AAOS
  installs a **separate artifact** on the car, so it needs its own copy of the
  data layer and its own manifest.
- Car App Library 1.7.0 is the current stable release. `MapWithContentTemplate`
  requires the `androidx.car.app.MAP_TEMPLATES` permission and the
  `androidx.car.app.ACCESS_SURFACE` permission, and hands the app a `Surface` to
  draw into via `SurfaceCallback`.

## Goals / Non-Goals

**Goals:**
- Deliver one car experience (map + alert list) that runs on both Android Auto and
  Android Automotive OS from shared sources.
- Reuse the existing repositories, handlers, alert computation, and osmdroid tile
  pipeline rather than writing car-specific data or map code.
- Keep the phone app's behavior and packaging intact.
- Keep the change incremental: every step compiles and existing tests keep passing.

**Non-Goals:**
- Turn-by-turn navigation, routing, or voice actions.
- Navigating from the car (no `NAVIGATION` category).
- A second phone UI or any change to `Main`/`MapFragment`.
- Media, messaging, or parked-app categories.
- Any settings UI in the car: the first iteration is read-only for the user's
  configured alert range, sound, and vibration. Changing car settings is a
  follow-up.

## Decisions

### Decision: Weather category with `MapWithContentTemplate`

Declare `androidx.car.app.category.WEATHER`, request `MAP_TEMPLATES` (not
`NAVIGATION_TEMPLATES`), and present maps with `MapWithContentTemplate`. The alert
list is the content template rendered beside the map.

- **Why**: The app shows live weather-related conditions for the current location,
  which is what the Weather category covers, and Weather is allowed on both
  Android Auto and AAOS. `MAP_TEMPLATES` (without navigation) is legal for weather
  apps; using `NAVIGATION_TEMPLATES` would require real navigation and would be
  rejected if both permissions are declared.
- **Alternative considered**: `NAVIGATION` category. Rejected: the app provides no
  turn-by-turn routing, and `NavigationTemplate` is restricted to navigation apps.
- **Alternative considered**: `POI`. Rejected: lightning strikes are events, not
  places, and POI review requires place search/details.
- **Risk noted**: category fit is an external review decision; see Risks.

### Decision: Split the codebase into `:core`, `:car`, `:automotive`

- `:core` — Android library. Home for the non-UI logic moved out of `:app`:
  `data`, `jsonrpc`, `location`, `alert`, `protocol`, `preferences`, `util`, and
  the Dagger modules that wire them. Keeps the existing package names
  (`org.blitzortung.android.*`) so imports and tests change minimally. The
  settings UI and `PreferencesBackup` stay in `:app` (they are only used by the
  settings screen and would drag the backup strings into the shared module for no
  car benefit).
- `:car` — Android library. The opt-in car UI and service: `CarAppService`,
  `Session`, `Screen`s, the map surface renderer, the alert-list presenter, and the
  car manifest entries common to both platforms (service + category + permissions).
  Depends on `:core` and `androidx.car.app:app`.
- `:app` — phone application. Depends on `:core` and `:car`, adds
  `androidx.car.app:app-projected` and the Android Auto
  `com.google.android.gms.car.application` metadata.
- `:automotive` — application. minSdk 29. Depends on `:core` and `:car`, adds
  `androidx.car.app:app-automotive`, the AAOS features/metadata, and the
  `CarAppActivity` entry point.

- **Why**: Google specifies a shared module plus a separate AAOS module because the
  manifests and distribution tracks differ, and an application module cannot be a
  dependency of another module — so the shared logic must live in a library. Keeping
  package names avoids touching every import.
- **Alternative considered**: Reuse `:app` sources from `:automotive` via custom
  `sourceSets`. Rejected: compiles phone UI/resources into the car APK, duplicates
  resource processing, and hides the module boundary.
- **Alternative considered**: Support Android Auto only (service inside `:app`) and
  defer AAOS. Rejected: the user asked for both; the `:core` split is the enabling
  step for AAOS.
- **Required decoupling (found while planning)**: the data/alert/location packages
  are not yet independent of the UI. `MainDataHandler` casts event sources to
  `OwnMapView` and drives grid/animation updates; `AlertHandler` depends on
  `app.controller.NotificationHandler`; and many files use `app.Main.LOG_TAG`,
  `app.R`, `app.view.PreferenceKey`, and the preference helpers in
  `app.view`. Extraction therefore requires inverting these first: move the log
  tag into a shared location, remove the `OwnMapView` and `NotificationHandler`
  dependencies behind the `MapViewport` and `AlertNotifier` ports, and relocate `PreferenceKey` and
  the preference helpers into the shared layer. This decoupling is part of group 1
  of the task list and is the reason the extraction is sequenced rather than done
  as one mechanical move.

### Decision: Render the car map by reusing osmdroid through a `VirtualDisplay`

Host an osmdroid `MapView` (with the strike and own-location overlays) in a
`Presentation` on a `VirtualDisplay` created from the host `Surface`, per the
supported "render Views into the Surface" pattern. Forward map interactions from
`SurfaceCallback` (`onScroll`, `onScale`, `onFling`, `onClick`) to the `MapView`
controller, and add a map action strip with `Action.PAN` plus zoom actions.

- **Why**: The chosen UX is a real basemap + strike overlay. Reusing osmdroid keeps
  tile fetching, caching, offline behaviour, and the existing age-coloring overlays
  instead of reimplementing a tile pipeline against the raw `Surface` `Canvas`.
- **Alternative considered**: Draw directly on the `Surface` `Canvas` with a custom
  tile layer. Rejected: duplicates osmdroid's tile cache, projection, and overlay
  drawing, and re-derives what `StrikeListOverlay` already does.
- **Alternative considered**: Reuse `MapFragment` directly. Rejected: it is bound
  to an Activity, the phone layout, and the app theme; a thin car-owned `MapView`
  setup is cleaner.
- **Fallback**: if a given host rejects the `Presentation` path, draw strikes and
  location directly on the `Surface` `Canvas` (no basemap) behind the same renderer
  interface. This is a fallback, not the primary path.

### Decision: Reuse the existing data/location/alert layer unchanged

The car screens observe `StrikeDataRepository`, `LocationRepository`, and
`AlertRepository` (and the same `MainDataHandler`/`AlertDataHandler` underneath) as
the phone UI. Android Auto uses the instance from `:app`'s Dagger graph; AAOS wires
the same `:core` graph in its own small Dagger component.

- **Why**: The repositories already expose cold `Flow`s of exactly the state the
  car needs (`DataReceived`, `StatusUpdate`, `Warning`, location), and reusing them
  preserves cache TTL, sequencing, and alert computation. It satisfies the
  "no independent fetching" requirement.
- **Alternative considered**: Give the car app its own fetch loop. Rejected:
  duplicates cache/sequence logic and would diverge from the phone behaviour.

### Decision: Car dependency injection via injected factories per application

`CarAppService`/`Session` live in `:car` and are framework-instantiated, so they
cannot be Dagger-constructed directly. `:car` defines a small provider interface
that each application registers at startup to hand over the repositories and
parameters; `:app` supplies them from `AppComponent`, `:automotive` from its own
component built on the `:core` modules.

- **Why**: Keeps `:car` free of a dependency on either application module while
  still using Dagger for the graph.
- **Alternative considered**: Put the whole graph (including `BOApplication`) in
  `:core`. Rejected: `AppComponent` binds `:app`-only activities/UI, so the
  component cannot move wholesale.

### Decision: Location through `CarContext`, mapped to the existing location flow

Request `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` through
`CarContext.requestPermissions`, then feed the result into the existing
`LocationRepository`. When denied, fall back to the configured manual/default
location.

- **Why**: Android Auto and AAOS grant location through the car host, not the
  phone's permission dialogs; routing it into the existing repository keeps one
  location path.

### Decision: Car App Library 1.7.0, AAOS module at minSdk 29

Pin `androidx.car.app:app`/`app-projected`/`app-automotive`/`app-testing` to
`1.7.0`. `:car` and `:core` keep minSdk 23 (Android Auto runs on phones); the
`:automotive` application raises minSdk to 29, matching the AAOS Car App Library
floor, and declares the `android.software.car.templates_host` feature.

- **Why**: 1.7.0 is the latest stable; the separate AAOS minSdk is required by the
  AAOS template host.

### Decision: Build the car UI on templates with light/dark support

Screens use Car App Library templates only. The map redraws for dark mode via
`CarContext.isDarkMode` and `Session.onCarConfigurationChanged`, satisfying the car
app quality requirement.

- **Why**: Arbitrary Views are not allowed in the car UI (except inside the
  approved `Presentation` map surface), and dark-theme support is a review
  requirement.

## Risks / Trade-offs

- **Category review could reject a lightning-only weather app** → Mitigate by
  satisfying the weather quality guidelines (current conditions/forecast framing,
  light/dark map, portrait + landscape) and by submitting to an internal/closed
  track first; if rejected, revisit POI/navigation or reduce to an alert-only
  weather experience. This is the single largest schedule risk and is external to
  the code.
- **`:core` extraction regressions** → The shared packages currently depend on UI
  types (`Main`, `OwnMapView`, `NotificationHandler`) and on app resources, so
  extraction needs a decoupling pass before any files move. Mitigate by decoupling
  and moving one concern at a time, keeping package names where possible, moving the
  corresponding tests alongside the sources, and running `./gradlew
  testDebugUnitTest` after every step.
- **`VirtualDisplay` + `Presentation` map may misbehave on some hosts** → Mitigate
  with the direct-`Canvas` fallback behind the same renderer interface, and test on
  both the DHU and the AAOS emulator.
- **Input forwarding lag or gesture conflicts** → Mitigate by adding the required
  `Action.PAN` and mapping `onScroll`/`onScale` to the controller; keep gesture
  handling isolated in the renderer.
- **Resource/overlay reuse pulls phone styling into the car** → Mitigate by using a
  car-specific minimal overlay set and a car-specific layout, not `MapFragment`.
- **Dependency locking and new modules** → New modules need committed lock files;
  regenerate with `--write-locks` and keep `settings-gradle.lockfile` in sync.
- **Dedicated AAOS track requirement** → Mitigate by documenting the release
  process; the AAOS artifact must never be uploaded to the mobile track.

## Migration Plan

1. Create `:core`; move the non-UI packages and their tests, keep package names,
   and make `:app` depend on `:core`. Verify `./gradlew testDebugUnitTest` and
   `assembleDebug` are green.
2. Add `:car` with the `CarAppService`, `Session`, screens, renderer, and alert-list
   presenter, plus the shared car manifest entries. Add `androidx.car.app` deps and
   regenerate lock files.
3. Wire Android Auto: `:app` depends on `:car` + `app-projected`, adds
   `automotive_app_desc.xml` and the `com.google.android.gms.car.application`
   metadata. Verify on the Desktop Head Unit.
4. Add `:automotive`: AAOS manifest, `CarAppActivity`, `app-automotive` dependency,
   own Dagger component on `:core`, and a car launcher entry. Verify on the AAOS
   emulator.
5. Add unit tests with `androidx.car.app:app-testing` for the screen/session state
   mapping, and car icon resources.
6. Distribution: Play Console form-factor opt-in, AAOS screenshots, dedicated AAOS
   track, and submit for car app quality review.

**Rollback**: remove the `:car`/`:automotive` modules, revert the `:app` manifest
and Gradle changes, and drop the `:core` module by moving the packages back into
`:app`. No stored state, preference keys, or data migrations are introduced, so
rollback is a pure code revert.

## Open Questions

- Which tile source the car map should use (existing osmdroid default vs a
  car-specific style) — safe to decide during implementation without changing the
  specs. The car reuses the existing osmdroid cache, so no separate offline story
  is introduced.
