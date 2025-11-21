# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project Overview

This is the **Blitzortung Lightning Monitor** Android application - a real-time lightning visualization app built in Kotlin that displays lightning strike data from the blitzortung.org network. The app features map-based strike visualization, proximity alerts, background monitoring, and localization support for 10+ languages.

Project documentation: https://blitzortung.tryb.de

## Build Commands

### Building the app
```bash
./gradlew build
```

### Running tests
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "org.blitzortung.android.alert.AlertResultTest"

# Run tests with coverage report
./gradlew testDebugUnitTest jacocoTestReport
# Coverage report: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Linting and code quality
```bash
# Run lint
./gradlew lint

# Run SonarQube analysis (requires SONAR_TOKEN)
./gradlew sonar

# Update the verification metadata
gradle --write-verification-metadata sha256
```

### Building release artifacts
```bash
# Build APK
./gradlew assembleRelease

# Build Android App Bundle (AAB)
./gradlew bundleRelease
```

### Installing on device
```bash
# Install debug build
./gradlew installDebug

# Install and launch
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.blitzortung.android.app/.app.Main
```

## Technology Stack

- **Language**: Kotlin 2.2.10, JVM 17
- **Min SDK**: 21, Target SDK: 35
- **Build System**: Gradle 8.13
- **DI Framework**: Dagger 2.57.1
- **Testing**: JUnit 4, AssertJ, MockK, Robolectric
- **Map Library**: OSMDroid 6.1.20
- **UI**: ViewBinding, Material Components, AndroidX libraries

## Code Architecture

### High-Level Structure

The app follows a **component-based architecture** with Dagger 2 dependency injection coordinating the major subsystems:

```
BOApplication (Dagger root)
    ├── Main Activity (foreground UI)
    │   ├── MapFragment with overlays
    │   ├── UI components (status, alerts, histogram, etc.)
    │   └── Controllers (history, notifications, buttons)
    └── AppService (background monitoring)
        └── Minimal UI (notifications only)

Shared Singletons:
    ├── MainDataHandler / ServiceDataHandler (data flow)
    ├── LocationHandler (GPS tracking)
    ├── AlertHandler (proximity alerts)
    └── DataProviderFactory (API clients)
```

### Dagger 2 Dependency Injection

**Component**: `AppComponent` (singleton scope)

**Modules**:
- `AppModule`: Application context, SharedPreferences, PackageInfo, NotificationManager, Vibrator, WakeLock
- `ServiceModule`: Handler (main thread), AlarmManager, Period (timing utilities)
- `ActivityBindingModule`: Auto-injects Main activity and AppService
- `SettingsModule`: Auto-injects SettingsFragment

All major components (`@Inject` annotated) are wired through Dagger. The `BOApplication` class initializes the component graph on startup.

### Event-Driven Communication (ConsumerContainer Pattern)

The app uses a **publish-subscribe pattern** via `ConsumerContainer<T>` for component communication:

**Event Types**:
- `DataEvent`: Strike data updates (ResultEvent, RequestStartedEvent, StatusEvent)
- `AlertEvent`: Alert state changes (AlertResultEvent, AlertCancelEvent)
- `LocationEvent`: GPS location updates

**Key Producers**:
- `MainDataHandler` / `ServiceDataHandler`: Broadcasts `DataEvent` when new strike data arrives
- `AlertHandler`: Broadcasts `AlertEvent` when proximity alerts trigger/cancel
- `LocationHandler`: Broadcasts `LocationEvent` when device location changes

**Consumers** register via `requestUpdates(consumer: (Event) -> Unit)` and receive callbacks when events are broadcast. The last event is cached and immediately sent to new consumers.

### Data Flow Architecture

#### Data Providers (Strategy Pattern)

Two interchangeable data provider implementations:

1. **JsonRpcDataProvider** (default): JSON-RPC over HTTP with incremental updates
2. **BlitzortungHttpDataProvider**: HTTP GET with GZIP compression, requires auth

Factory: `DataProviderFactory` selects provider based on user preference

#### MainDataHandler (Data Orchestrator)

- Manages data fetch lifecycle and coordinates updates
- **Caching**: `DataCache` with 5-minute TTL to avoid redundant API calls
- **Sequencing**: `SequenceValidator` prevents stale data from overwriting fresh data
- **Animation mode**: Special handling for historical playback
- Responds to location changes by updating grid parameters
- Publishes `DataEvent` to all registered consumers

**Typical flow**: Timer triggers → updateData() → check cache → FetchDataTask (background) → API call → cache result → broadcast ResultEvent → consumers update UI/alerts

### Alert System Architecture

**AlertHandler**: Coordinates alert monitoring

- Subscribes to `DataEvent` (strike updates) and `LocationEvent` (position changes)
- Delegates computation to `AlertDataHandler`
- Broadcasts `AlertEvent` to UI components and `AlertSignal` (vibration/sound)

**AlertDataHandler**: Core alert logic

- Divides 360° into sectors (default 8: N, NE, E, SE, S, SW, W, NW)
- Each sector has range buckets (10km, 25km, 50km, 100km, 250km, 500km)
- For each strike: calculates bearing + distance, assigns to sector/range
- Tracks closest strike in configurable time window (default 10 minutes)
- Returns `AlertResult` with sector data and closest strike distance/bearing

**AlertResult**: Contains sector-wise strike counts and closest strike metadata for UI display

### Map Overlay System

**Overlays** (drawn in order):
1. `FadeOverlay`: Applies alpha transparency based on strike age
2. `StrikeListOverlay`: Renders individual strikes with age-based coloring (red→yellow→blue)
3. `OwnLocationOverlay`: Shows user's current position

**Color Handlers**:
- `StrikeColorHandler`: Maps strike timestamp to color gradient
- Grid boundary rendering when in grid mode

Overlays subscribe to events for automatic updates:
- `DataEvent` → add/expire strikes
- `LocationEvent` → update position marker
- `ZoomEvent` → adjust rendering detail

### Component Lifecycle

**Main Activity (foreground)**:
- `onResume()`: Registers all consumers, starts location updates, enables automatic data refresh
- `onPause()`: Unregisters consumers, stops location updates, optionally starts AppService for background alerts

**AppService (background)**:
- Runs as foreground service with notification
- Minimal UI updates (no map rendering)
- Scheduled via AlarmManager for periodic data fetches
- Only active when background alerts are enabled

## Package Structure

```
org.blitzortung.android/
├── alert/              Alert computation, sector handling, alert events
├── app/                Main activity, AppService, boot receiver, UI controllers
│   ├── components/     Version checking, changelog
│   ├── controller/     History, notifications, button column
│   ├── permission/     Runtime permission requesters
│   └── view/           Custom views (AlertView, HistogramView, LegendView, etc.)
├── dagger/             DI component and modules
├── data/               Data models, caching, data handlers, fetch tasks
│   ├── beans/          Strike, Station, GridElement, Location
│   ├── cache/          DataCache implementation
│   └── provider/       DataProvider interface, factory, implementations
├── dialogs/            Info, log, alert, quick settings dialogs
├── jsonrpc/            JSON-RPC client for API communication
├── location/           LocationHandler, location providers
├── map/                MapFragment, overlays, color handlers
├── protocol/           Event interfaces, ConsumerContainer
├── settings/           SettingsFragment, preference handling
├── util/               Time formatting, measurement systems, period utilities
└── widget/             App widget provider
```

## Testing Strategy

**Test Framework**: JUnit 4 + Robolectric for Android components

**Key Test Utilities**:
- `MockK`: Mocking framework (preferred over Mockito for Kotlin)
- `AssertJ`: Fluent assertions
- `Robolectric`: Android framework emulation for unit tests

**Test Patterns**:
- Mock external dependencies (SharedPreferences, Context, API clients)
- Use `@Before` for common setup
- Test public interface behavior, not implementation details

**Running Single Tests**: Use `--tests` flag with fully qualified test name:
```bash
./gradlew testDebugUnitTest --tests "org.blitzortung.android.data.ParametersTest"
```

## Important Patterns and Conventions

### SharedPreferences Integration

Many components implement `OnSharedPreferenceChangeListener` to react to settings changes. Register in `onResume()`, unregister in `onPause()`.

### ViewBinding

All activities/fragments use ViewBinding (enabled in build.gradle). Access views via `binding.viewId` instead of `findViewById()`.

### Threading Model

- **Main thread**: UI updates, Handler callbacks
- **Background threads**: Network I/O (FetchDataTask), WorkManager tasks
- Use `Handler.post()` to marshal callbacks to main thread

### Location Handling

Location providers are abstracted via `LocationProvider` interface. Implementations:
- `GPSLocationProvider`: Device GPS
- `ManualLocationProvider`: User-specified coordinates
- `NetworkLocationProvider`: Network-based location

### Preference Keys

Preference keys are defined in `PreferenceKey` enum. Use these constants instead of hardcoded strings.

## CI/CD

GitHub Actions workflows:
- **build.yml**: Runs on push/PR, executes tests + coverage + SonarQube analysis
- **dependabot.yml**: Automated dependency updates
- **scorecard.yml**: OpenSSF security scorecard
- **dependency-review.yml**: Dependency vulnerability scanning

## Development Notes

- **Code Coverage**: Generated by JaCoCo, viewable at `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- **Lint Reports**: Generated at `app/build/reports/lint-results-debug.xml`
- **minSdkVersion 21**: Minimum Android 5.0 (Lollipop)
- **Translations**: Strings in `res/values-{locale}/strings.xml` (10+ languages supported)
- **ChangeLog**: Parsed from `ChangeLog` file and displayed in app on first run after update

## Common Tasks

### Adding a new data provider
1. Implement `DataProvider` interface in `data.provider.data/`
2. Register in `DataProviderFactory`
3. Add corresponding `DataProviderType` enum value
4. Update SharedPreferences to allow selection

### Adding a new alert feature
1. Modify `AlertParameters` to include new configuration
2. Update `AlertDataHandler.checkStrikes()` logic
3. Extend `AlertResult` if new metadata needed
4. Update `AlertView` to display new information

### Adding a new map overlay
1. Extend `Overlay` class and implement `LayerOverlay` marker interface
2. Implement `draw()` method for canvas rendering
3. Optionally implement `MapListener` for zoom/scroll events
4. Register overlay in `Main.onStart()` and add to `mapView.overlays`
5. Subscribe to relevant events for data updates

### Adding a new preference
1. Add entry to `PreferenceKey` enum
2. Add XML definition in `res/xml/preferences.xml`
3. Implement `OnSharedPreferenceChangeListener` in affected components
4. Update default values in `PreferenceManager.setDefaultValues()` call
