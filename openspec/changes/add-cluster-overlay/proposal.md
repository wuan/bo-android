## Why

The bo-srv-rs backend now exposes strike cluster data (`get_global_clusters` / `get_local_clusters`) that the app cannot use yet. Clusters give a summarized, lower-bandwidth view of storm activity as geographic outlines, which is useful both as an alternative to the point/grid view and as an additive layer on top of it.

## What Changes

- Add a new **cluster data transmission** that fetches cluster outlines from the RPC backend, mirroring the existing global/local grid pattern (local clusters use the same map tile coordinates; global clusters are used at low zoom).
- Add a **ClusterOverlay** that renders clusters as static polygon outlines, independently toggleable while the existing strike/grid view stays visible.
- Add cluster-specific preferences: an enable/disable toggle and a cluster interval (5 or 10 minutes) covering the last hour.
- Introduce a **parallel data event stream** for clusters (separate from the strike `DataReceived` stream) so cluster fetching is independently configurable and can be toggled without refetching strike data.
- Add a **grid/cluster data selection** to the quick settings dialog: the strike count threshold control is removed and replaced by two independent toggles for the grid (strike) overlay and the cluster overlay.
- Make the **grid data deactivateable**: a `SHOW_GRID` preference hides the strike/grid overlay (rendering only) while strike data continues to be fetched, so alerts, the histogram and the time slider keep working.
- Gate the feature to the RPC data source; the HTTP provider does not serve clusters.
- Out of scope: proximity alerts based on clusters, cluster animation/history playback, and trails/motion.

## Capabilities

### New Capabilities
- `cluster-overlay`: Fetching cluster data from the RPC backend (global/local, configurable 5/10 minute interval over the last hour), exposing it through a reactive stream, and rendering it as a static outline overlay that can be shown simultaneously with the point/grid view.

### Modified Capabilities
<!-- No existing spec requirements change; cluster support is additive. -->

## Impact

- `data/provider`: `DataProvider.DataRetriever` gains a cluster retrieval method; `JsonRpcDataProvider` + `JsonRpcData` gain `get_global_clusters`/`get_local_clusters` calls; RPC capability flag.
- `data/beans`: new `Cluster` bean (polygon shape + strike count/area/timestamp).
- `data`: new `ClusterReceived` event, new `DataChannel.CLUSTERS`, cluster parameters, cluster cache handling.
- `data/MainDataHandler`: fire a second fetch for clusters alongside strike data; local/global selection needs a zoom/bounding-box criterion (clusters have no `grid_size`).
- `data/repository` + `app/viewmodel`: `ClusterRepository` and cluster flow exposed via `MainViewModel`.
- `map/overlay`: new `ClusterOverlay` + `ClusterShape`; color by cluster age.
- `settings` / `PreferenceKey` / `res/xml/preferences.xml`: cluster toggle and interval preferences.
- `settings`: `SHOW_GRID` display preference (default on) alongside `SHOW_CLUSTERS`.
- `dialogs/QuickSettingsDialog` / `res/layout/quick_settings_dialog.xml`: remove the strike count threshold control, add independent grid and cluster display toggles with boolean persistence.
- `Main`/`MapFragment`: overlay registration and lifecycle wiring; grid/cluster overlay visibility gated on the display preferences without changing the data-fetch path.
