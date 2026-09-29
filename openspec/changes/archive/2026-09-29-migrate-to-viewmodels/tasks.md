## 1. Build & DI scaffolding

- [x] 1.1 Add `androidx.lifecycle:lifecycle-viewmodel-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`, and `kotlinx-coroutines-android` to `app/build.gradle.kts` (versions compatible with Kotlin 2.2.10) and update dependency verification metadata if required
- [x] 1.2 Create `app/viewmodel/ViewModelFactory.kt` injecting `Map<Class<out ViewModel>, Provider<ViewModel>>` and throwing `IllegalArgumentException` for unknown classes
- [x] 1.3 Create `dagger/module/ViewModelModule.kt` with `@ViewModelKey` + `@Binds @IntoMap` bindings for `MainViewModel`, `MapViewModel`, `SettingsViewModel`, and bind `ViewModelFactory` as `ViewModelProvider.Factory`
- [x] 1.4 Register `ViewModelModule` in `dagger/component/AppComponent.kt`
- [x] 1.5 Verify the project compiles: `./gradlew assembleDebug`

## 2. Repository layer

- [x] 2.1 Create `data/repository/StrikeDataRepository.kt` wrapping `MainDataHandler` with `observeDataEvents(): Flow<DataEvent>` via `callbackFlow`/`awaitClose` and forwarding data commands (`updateData`, `start`, `stop`, `restart`, `startAnimation`, `goRealtime`, `setPosition`, `historySteps`, `toggleExtendedMode`, `getParameters`, `getIntervalDuration`, `isRealtime`, `calculateTotalCacheSize`)
- [x] 2.2 Create `data/repository/ServiceStrikeDataRepository.kt` wrapping `ServiceDataHandler` with the same Flow/command contract
- [x] 2.3 Create `location/LocationRepository.kt` wrapping `LocationHandler` with `observeLocationEvents()`, `getCurrentLocation()`, `enableBackgroundMode()`, `disableBackgroundMode()`
- [x] 2.4 Create `alert/AlertRepository.kt` wrapping `AlertHandler` with `observeAlertEvents(): Flow<Warning>` and `getAlertParameters()`
- [x] 2.5 Add unit tests for each repository covering event forwarding, consumer release on cancellation, and command delegation
- [x] 2.6 Run `./gradlew testDebugUnitTest` for the repository tests

## 3. Settings ViewModel & screen

- [x] 3.1 Create `app/viewmodel/SettingsViewModel.kt` exposing `preferenceChanged: SharedFlow<PreferenceKey>`, typed get/set helpers, and listener registration/release
- [x] 3.2 Add unit tests for preference mapping (known/unknown keys), typed reads/writes, and listener cleanup
- [x] 3.3 Migrate `SettingsFragment` to obtain `SettingsViewModel` via `viewModels { factory }` and handle preference changes reactively inside `repeatOnLifecycle(STARTED)`
- [x] 3.4 Run `./gradlew testDebugUnitTest` and manually verify settings changes still apply

## 4. Map ViewModel & screen

- [x] 4.1 Create `app/viewmodel/MapViewModel.kt` exposing `zoomLevel`, `centerPosition`, `mapType`, `isMapReady` `StateFlow`s plus `updateZoomLevel`, `updateCenterPosition`, `updateMapType`, `setMapReady`, `saveMapState`
- [x] 4.2 Add unit tests for map state updates and save/restore
- [x] 4.3 Add `AndroidSupportInjection.inject(this)` to `MapFragment` and obtain `MapViewModel` via `viewModels { factory }`
- [x] 4.4 Restore zoom/center from the ViewModel when they differ from the current map values and save zoom/center on zoom, scroll, and `onPause`
- [x] 4.5 Run `./gradlew testDebugUnitTest` and verify map state across rotation

## 5. Main ViewModel, HistoryController & Main

- [x] 5.1 Create `app/viewmodel/MainViewModel.kt` with `isLoading`, `hasError`, `currentResult`, `clearDataRequested`, and `dataEvents`/`locationEvents`/`alertEvents` `StateFlow`s derived from the repositories
- [x] 5.2 Port `Main`'s `DataReceived` handling into `MainViewModel` observers (loading/error transitions, failed results do not replace `currentResult`) and add command delegations plus `onCleared` stop
- [x] 5.3 Add unit tests for `MainViewModel` state transitions and command delegation
- [x] 5.4 Change `HistoryController` to depend on `MainViewModel` instead of `MainDataHandler` and update its `dataConsumer`/command call sites
- [x] 5.5 Migrate `Main` to inject `ViewModelProvider.Factory`, create `MainViewModel`, and collect its flows inside `repeatOnLifecycle(STARTED)`, porting the strike-overlay/history/status/region processing
- [x] 5.6 Remove manual `requestUpdates`/`removeUpdates` registrations from `Main` (including the defective `disableDataUpdates()` path) and delete now-unused consumer fields
- [x] 5.7 Run `./gradlew testDebugUnitTest` and manually verify strikes, history playback, location, and alerts

## 6. Background service migration

- [x] 6.1 Migrate `AppService` to use `ServiceStrikeDataRepository`/repositories for data and alert wiring instead of manual `ServiceDataHandler`/`LocationHandler` consumer registration
- [x] 6.2 Ensure `AppService.onDestroy()` releases preference listeners and subscriptions
- [x] 6.3 Run `./gradlew testDebugUnitTest` and verify background alerts and periodic updates

## 7. Verification & cleanup

- [x] 7.1 Run the full unit test suite: `./gradlew testDebugUnitTest`
- [x] 7.2 Run lint: `./gradlew lint` and resolve new warnings
- [x] 7.3 Remove or update the outdated `VIEWMODEL_MIGRATION_EXAMPLE.md` / `VIEWMODEL_MIGRATION_QUICK_REFERENCE.md` to match the current event model
- [x] 7.4 Update `AGENTS.md`/architecture docs to describe the repository + ViewModel layer and the new packages
