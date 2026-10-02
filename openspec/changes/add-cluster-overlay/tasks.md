## 1. Contract spike and data model

- [x] 1.1 Query `get_global_clusters` and `get_local_clusters` against the RPC backend for a representative global and local view; capture payload sizes, cluster count per hour, and vertices per `shape` (findings in design.md: cluster methods validated; dev `strike_clusters` empty so vertex counts unmeasured; global grid payload ~19 KB)
- [x] 1.2 Confirm `shape` semantics (closed ring, winding, holes) and which field drives age (`timestamp` vs `interval_seconds`); record findings in design.md open questions
- [x] 1.3 Add `Cluster` bean (id, timestamp, intervalSeconds, strikeCount, area, shape as list of lon/lat pairs)
- [x] 1.4 Add `ClusterParameters` (global/local flag, `dataArea`, `minuteLength`, `intervalCount`, `minuteOffset`)

## 2. Provider layer

- [x] 2.1 Add a cluster retrieval method to `DataProvider.DataRetriever`
- [x] 2.2 Add a `supportsClusters` capability flag to `DataProvider` (`true` for RPC, `false` for HTTP)
- [x] 2.3 Implement `get_global_clusters` / `get_local_clusters` calls in `JsonRpcData`
- [x] 2.4 Implement cluster response parsing in `JsonRpcDataProvider` (dedicated path, not `addGridData`)

## 3. Local/global selection

- [x] 3.1 Define the zoom/bounding-box criterion for switching between global and local cluster requests (D3), reusing the existing `DataArea`/tile computation
- [x] 3.2 Reuse `LocalData` tile coordinates (`x`, `y`, `scale`) for local cluster requests

## 4. Event stream and repository

- [x] 4.1 Add `DataChannel.CLUSTERS` and a `ClusterReceived` event
- [x] 4.2 Add a cluster cache with a shorter-than-strike TTL
- [x] 4.3 Fire a cluster fetch in `MainDataHandler` alongside strike data when cluster display is enabled and the provider supports clusters
- [x] 4.4 Add `ClusterRepository` adapting cluster events to a Flow (`callbackFlow`/`awaitClose`), following `StrikeDataRepository`
- [x] 4.5 Expose the cluster flow from `MainViewModel` (and wire `ViewModelModule`/factory if needed)

## 5. Preferences

- [x] 5.1 Add `SHOW_CLUSTERS` (default off) and `CLUSTER_INTERVAL` (last hour vs most recent only, default last hour) to `PreferenceKey`
- [x] 5.2 Add preference entries to `res/xml/preferences.xml` with defaults
- [x] 5.3 Handle preference changes in `MainDataHandler` (enable/disable fetch, reconfigure interval)
- [x] 5.4 Hide/disable cluster preferences and suppress cluster requests when the active provider does not support clusters
- [x] 5.5 Add user-facing strings (and translations where feasible)

## 6. Rendering

- [x] 6.1 Add `ClusterShape` rendering a polygon outline from `shape`
- [x] 6.2 Add `ClusterOverlay` (osmdroid `Overlay`, `LayerOverlay`) drawing all clusters, colored by age via the existing color handler
- [x] 6.3 Register the overlay in the map/`Main` lifecycle and update it from the cluster flow within `repeatOnLifecycle`
- [x] 6.4 Clear/remove rendered clusters when display is disabled or data is cleared

## 7. Verification

- [x] 7.1 Unit tests for cluster parsing (`Cluster` from JSON response)
- [x] 7.2 Unit tests for cluster parameter/interval derivation (`interval_count = 60 / interval`, history offset)
- [x] 7.3 Test provider capability gating (HTTP issues no cluster request)
- [ ] 7.4 Manual verification: clusters shown simultaneously with grid/point view; toggle on/off; last hour vs most recent only; global and local zoom
- [x] 7.5 Run `./gradlew testDebugUnitTest` and `./gradlew lint`

## 8. Data view selection and grid deactivation

- [x] 8.1 Add `SHOW_GRID` (boolean, default on) to `PreferenceKey` and `res/xml/preferences.xml`
- [x] 8.2 Remove the strike count threshold control from `res/layout/quick_settings_dialog.xml` and `QuickSettingsDialog`
- [x] 8.3 Add independent grid and cluster display toggles to the quick settings dialog with boolean persistence
- [x] 8.4 Gate `StrikeListOverlay`/`FadeOverlay` rendering on `SHOW_GRID` in `Main`, leaving the data-fetch path unchanged
- [x] 8.5 Disable/hide the cluster toggle in quick settings when the active provider does not support clusters
- [x] 8.6 Add user-facing strings for the new toggles
