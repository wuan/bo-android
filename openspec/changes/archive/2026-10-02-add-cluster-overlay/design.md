## Context

The RPC backend (bo-srv-rs) exposes two cluster methods:

```
get_global_clusters(minute_length, minute_offset = 0, interval_count = 1)
get_local_clusters(x, y, minute_length = 60, minute_offset = 0, data_area = 5, interval_count = 1)

→ { t, dt, clusters: [ { id, timestamp, interval_seconds, strike_count, area, shape } ] }
# shape = list of [lon, lat] pairs (polygon outline)
```

Clusters are stored server-side and produced every minute. `interval_count` returns multiple consecutive intervals in one response, so a single request (`minute_length = 10`, `interval_count = 6`) already covers the last hour.

The app's current data pipeline is strike-centric and single-result:
`MainDataHandler.updateData()` → `FetchDataTask` → `DataProvider.retrieveData { getStrikes | getStrikesGrid }` → one `DataReceived` event broadcast via `ConsumerContainer` and adapted to Flows by `StrikeDataRepository` → `MainViewModel`. Overlays (`StrikeListOverlay`, `FadeOverlay`, `OwnLocationOverlay`) consume `DataEvent`s.

Relevant existing mechanics:
- `LocalData.updateParameters()` picks local (`region = -1`, `DataArea(x, y, scale)`) vs global (`region = 0`) for grid data, gated on `parameters.gridSize < LOCAL_REGION_GRID_SIZE_THRESHOLD`.
- `DataCache` is keyed by `Parameters` with a 5-minute TTL.
- `DataMode(grid, region)` is set by provider type: RPC → grid, HTTP → points. `toggleExtendedMode()` flips `grid`.
- `BlitzortungHttpDataProvider` cannot serve clusters.

## Goals / Non-Goals

**Goals:**
- Fetch global/local cluster outlines from the RPC backend using the existing global/local tile pattern.
- Render clusters as static, outline-only polygons, colored by cluster age, toggleable independently of the strike/grid view.
- Provide a cluster display interval of either 10-minute bins covering the last hour or the most recent interval only.
- Keep cluster fetching on a parallel, independently configurable path so it can be enabled/disabled without refetching strike data.

**Non-Goals:**
- Proximity alerts based on clusters.
- Cluster trails / motion.
- Cluster support for the HTTP (`BlitzortungHttpDataProvider`) data source.
- Filled/heatmap rendering or cluster detail popups.

## Decisions

### D1: Parallel cluster stream rather than co-locating clusters in `DataReceived`

Add a `ClusterReceived` event on a new `DataChannel.CLUSTERS`, produced by a second `FetchDataTask`-style fetch in the same update tick, and adapted to a Flow by a new `ClusterRepository`. `DataReceived` stays unchanged.

- **Why**: clusters have their own interval configuration and must be toggled independently. Co-locating them in `DataReceived` would couple cluster fetching to strike parameters/caching and force a strike refetch on toggle.
- **Alternatives considered**: (A) add `clusters: List<Cluster>?` to `DataReceived` — simpler plumbing but binds the two lifecycles and complicates independent caching; rejected.

### D2: Cluster-specific parameter + cache handling

Introduce a `ClusterParameters` value (region/global-local, `dataArea`, `minuteLength`, `intervalCount`, `minuteOffset`) and a dedicated cache path with a TTL shorter than the strike 5-minute TTL (clusters update every minute). The existing strike-keyed `DataCache` is left untouched.

- **Why**: `DataCache` is keyed by the strike-centric `Parameters`; adding cluster fields there would pollute that type and risk key collisions with strike entries.
- **Alternatives considered**: overload `Parameters` with nullable cluster fields — rejected for the same reason.

### D3: Local/global selection for clusters uses zoom, not `grid_size`

Clusters have no `grid_size`. Add a zoom-level (or bounding-box extent) criterion, reusing the same zoom thresholds/`DataArea` computation as `LocalData`, so that the local cluster endpoint is used when zoomed in and the global endpoint otherwise.

- **Why**: the server's local cluster endpoint requires tile coordinates (`x`, `y`, `data_area`) and the global endpoint must be used at wide zoom; `grid_size` is unavailable to drive the existing switch.
- **Alternatives considered**: reuse `parameters.gridSize` artificially — rejected because clusters are independent of grid resolution and the auto-grid-size logic would not apply.

### D4: RPC-only capability gating

`DataProvider` exposes a capability flag (e.g. `supportsClusters`) that is `true` for `JsonRpcDataProvider` and `false` for `BlitzortungHttpDataProvider`. The cluster preference is hidden/disabled when the active provider does not support clusters, and no cluster fetch is issued for HTTP.

- **Why**: `get_global_clusters`/`get_local_clusters` exist only in the RPC backend; issuing them against the HTTP source would fail.

### D5: Static outline overlay colored by age

`ClusterOverlay` renders each cluster's `shape` ring as an outline using the osmdroid `Overlay`/`Shape` pattern (analogous to `StrikeListOverlay` + `StrikeShape`/`GridShape`). Stroke color is derived from cluster age via the existing `StrikeColorHandler` gradient; no fill, no popup, no hit-testing in this iteration.

- **Why**: matches the agreed first slice (outline-only, static) and reuses the established age-color behavior users already know from strikes.
- **Alternatives considered**: filled polygons shaded by `strike_count` — deferred; hit-test/popup — deferred.

### D6: Cluster interval as new preferences

Add `SHOW_CLUSTERS` (boolean, default off) and `CLUSTER_INTERVAL` (default "latest"), offering either the most recent interval only (`latestOnly`, `interval_count = 1`) or 10-minute bins over the last hour (`interval_count = 6`). Do not overload the existing `INTERVAL_DURATION` strike preference.

- **Why**: the cluster interval is a distinct axis from the strike window; overloading would create confusing coupling and break strike behavior. Restricting the choice to "last hour" vs "most recent only" keeps the UI simple while `ClusterParameters.latestOnly` expresses the single-interval case.
- **Alternatives considered**: reuse `INTERVAL_DURATION` — rejected; keeping a free 5/10-minute selection — rejected in favour of the two meaningful display modes.

### D7: Parsing isolation

Cluster parsing lives in a dedicated builder path and must not go through `JsonRpcDataProvider.addGridData`, which mutates global grid responses (`data.put("y1", 0.0)`, `data.put("x0", 0.0)`).

- **Why**: those mutations are grid-response-specific and would corrupt cluster parsing.

### D8: Independent grid and cluster visibility; grid deactivation is render-only

Expose the grid/strike overlay and the cluster overlay as two independent display toggles (`SHOW_GRID`, default on; `SHOW_CLUSTERS`, default off), surfaced as checkboxes in the quick settings dialog. The strike count threshold control is removed from the quick settings dialog because it is a grid-only tuning knob that no longer needs to be next to the new display selection. `SHOW_GRID` toggles only the `StrikeListOverlay` rendering; it does not alter `MainDataHandler`, parameters, or the data-fetch path. `FadeOverlay` stays independent of `SHOW_GRID`: it implements the dark/bright map appearance, so hiding the grid while showing clusters must not reveal the undimmed base tiles.

- **Why**: the user asked for grid and cluster selection, with grid deactivateable. Keeping deactivation render-only preserves the existing strike pipeline, so alerts, the histogram and the time slider keep receiving data even when the grid is hidden.
- **Alternatives considered**: (A) stop grid/strike requests when hidden — rejected because alerts and the histogram depend on strike data; (B) a single exclusive "grid XOR clusters" selector — rejected in favour of independent toggles per the chosen behaviour.

## Risks / Trade-offs

- **Payload size / vertex count at continental scale** → Cluster rings may be large and numerous; this is the biggest mobile risk. Mitigation: first slice is global-only, outline-only; measure response sizes; consider a later server-side vertex reduction or client-side level-of-detail. Flag as open question.
- **Global↔local switching without `grid_size`** → Risk of fetching the wrong flavour or thrashing at a zoom boundary. Mitigation: reuse the existing zoom thresholds and debounce through the same update tick as grid changes.
- **Two concurrent fetches per update tick** → Added network/latency and cache pressure. Mitigation: cluster fetch is skipped when disabled; smaller/shorter-TTL cache; single-flight if needed.
- **Cluster shape semantics** (closed ring? winding? holes?) → Could break fill/hit-test later. Mitigation: outline-only now makes these largely irrelevant; confirm contract before adding fill/popups.
- **Cache TTL mismatch** → 5-minute strike TTL is too coarse for per-minute clusters. Mitigation: dedicated short TTL (D2).
- **Animation coupling** → `Mode.ANIMATION` steps `minute_offset` for strikes. Clusters follow the same stepped `minuteOffset` by re-issuing a cluster fetch each animation step; the short cluster cache TTL bounds repeated network cost across cycles.

## Migration Plan

Additive and behind an off-by-default preference; no data migration. Rollback is limited to disabling the `SHOW_CLUSTERS` preference / reverting the change. The strike and grid paths are untouched.

## Open Questions

### Resolved by contract spike (2026-10-01)

Live probe of `http://bo-service.tryb.de/` plus the authoritative `bo-srv-rs` source
(`src/data.rs`, `src/service.rs`, `src/db.rs`, `README.md`):

- **Endpoint access requires `User-Agent: bo-android-<int>` and `Content-Type: text/json`.**
  The service blocks any other user agent and answers with an empty `{}` result
  (indistinguishable from no data). All spike requests must set the agent.
- **`shape` semantics**: confirmed a **closed outer ring** as a GeoJSON `LineString`
  coordinate list of `[lon, lat]` pairs (`StrikeCluster.shape: Option<Vec<(f64,f64)>>`,
  "the rounded exterior ring"). It is **not** a Polygon, has **no holes**, and the
  first point equals the last (shapely exterior ring). When no shape could be built,
  `shape` is an **empty list** and `area` is `null`.
- **Age field**: `timestamp` (the interval timestamp) drives age coloring.
  `interval_seconds` is the interval **length** (e.g. 600 for 10 minutes), not an age.
- **Result schema** (confirmed against live response for both cluster methods):
  `{ t, dt, clusters: [ { id, timestamp, interval_seconds, strike_count, area, shape } ] }`
  where `t` = interval end formatted `%Y%m%dT%H:%M:%S`, `dt` = interval seconds, and
  each cluster `timestamp` is formatted `%Y-%m-%d %H:%M:%S.%f` (UTC, 9 fractional digits).
- **Dev-server payload**: the dev `strike_clusters` table is currently empty, so
  `clusters` was `[]` for every global and local probe (response ~82 bytes). The
  global grid payload for `minute_length=60, grid_base_length=25000` was ~19 KB.
  Vertex count per cluster could not be measured against live data and remains a
  production risk; first slice stays outline-only with level-of-detail deferred.

### Still open

- How many `[lon, lat]` vertices per cluster, and how many clusters per hour at global scale? Determines whether client-side simplification/LOD is required for the first slice.
- Concrete zoom threshold for the global↔local cluster switch, and whether it should diverge from the grid threshold.
- Whether cluster fetching should share the strike `updateData()` tick or run on an independent period.
- Whether cluster responses can be empty for large regions and how that should render (no-op vs clear).
