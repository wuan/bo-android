# Tasks

## 1. Cluster request parameters

- [x] 1.1 Replace the `latestOnly` + `CLUSTER_RANGE_MINUTES` + `intervalCountFor()` derivation in `ClusterParameters` with an explicit `intervalCount` (and a `CLUSTER_BIN_MINUTES = 10` constant), and update `ClusterParametersTest` to assert counts directly; verify with `./gradlew testDebugUnitTest --tests "org.blitzortung.android.data.ClusterParametersTest"`
- [x] 1.2 Update `LocalData.clusterParameters(...)` to carry the resolved interval count through to `ClusterParameters` while keeping the global/local + `dataArea` selection unchanged; verify the cluster cases in `./gradlew testDebugUnitTest --tests "org.blitzortung.android.data.provider.standard.JsonRpcDataProviderTest"` pass

## 2. Handler wiring

- [x] 2.1 In `MainDataHandler`, replace `CLUSTER_INTERVAL_HOUR` with a `CLUSTER_INTERVAL_MATCH = "match"` constant and map the preference so any value other than `latest` (including the legacy `hour`) resolves to "match"; verify by extending `MainDataHandlerTest` to cover `latest`, `match` and legacy `hour`
- [x] 2.2 In `MainDataHandler.resolveClusterIntervalCount()`, compute the interval count as `latest ? 1 : ClusterParameters.intervalCountFor(parameters.intervalDuration)` and attach it to the cluster parameters (keeping `withMinuteOffset(parameters.intervalOffset)`); verify a test asserts the count is 3 for a 30-minute time interval and 12 for 120 minutes

## 3. Preferences and resources

- [x] 3.1 Update the `cluster_intervals` / `cluster_interval_values` string arrays to "Match time interval" (`match`) and "Most recent only" (`latest`), remove "Last hour", and set `preferences.xml` `android:defaultValue="match"`; verify the settings screen opens and shows the two entries via `./gradlew testDebugUnitTest --tests "org.blitzortung.android.settings.SettingsFragmentTest"`
- [x] 3.2 Confirm a `cluster_interval` value of `match`/`latest`/`hour` all round-trip through `PreferencesBackup` as a string and that `hour` resolves to "match" at read time; verify with a `PreferencesBackupApplyTest` case covering the legacy value

## 4. Integration verification

- [x] 4.1 Run `./gradlew testDebugUnitTest` and verify the full unit-test suite passes
- [x] 4.2 Run `./gradlew lint` and verify no new findings are introduced
- [x] 4.3 Manual check against the RPC source: enable clusters and verify the requested window and outline color scale match the time interval at 30 and 120 minutes, "Most recent only" fetches a single bin, history offset and animation still step clusters, and a stored legacy `hour` value behaves as "match"

## Workflow follow-up

- Archive the change once implementation is reviewed and merged.
