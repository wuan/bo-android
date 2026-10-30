# Proposal: Match cluster interval to the time interval

## Why

The cluster overlay fetches a hardcoded 60-minute window regardless of the user's
**Time interval** setting (`INTERVAL_DURATION`), and defaults to "Most recent only"
(a single 10-minute bin). Because `ClusterOverlay` colors clusters against the span it
actually fetched (`interval_seconds × interval_count`), the cluster color scale drifts
away from the strike legend whenever the time interval is anything other than 60
minutes (30, 90, 120, 240). The user sees the same color gradient meaning two different
time ranges. Aligning the cluster window with the selected time interval removes the
drift and makes "Match time interval" the sensible default.

## What Changes

- Replace the `CLUSTER_INTERVAL` choice set: **"Match time interval" (new default)** and
  **"Most recent only"**; remove **"Last hour"** (subsumed by Match at the default
  60-minute interval).
- Derive the cluster request window from the selected time interval: for **Match**,
  `interval_count = intervalDuration / CLUSTER_BIN_MINUTES`; for **Most recent only**,
  `interval_count = 1`. Bin length stays fixed at 10 minutes (`minute_length = 10`).
- Remove the hardcoded 60-minute range that currently pins the window
  (`ClusterParameters.CLUSTER_RANGE_MINUTES` and the `intervalCountFor(minuteLength)`
  derivation), and carry the resolved `intervalCount` on `ClusterParameters` instead.
- Cluster age coloring now spans the selected time interval, matching the strike
  legend, for every time interval in the current list (30/60/90/120/240 → 3/6/9/12/24
  bins). The `ClusterOverlay` age-to-color computation is unchanged; it simply receives
  a window that equals the time interval.
- Treat the legacy `"hour"` preference value as the new default so upgraded installs
  and restored backups do not resolve to an unknown value.
- **BREAKING (behavioral)**: an install that never changed the cluster interval moves
  from a 10-minute window to a window equal to the (default 60-minute) time interval.

## Capabilities

### New Capabilities

None. The behaviour belongs to the existing cluster overlay capability.

### Modified Capabilities

- `cluster-overlay`: the "Configurable cluster interval over the last hour" requirement
  and the "Cluster preference gating" requirement change. Cluster interval becomes
  "match the current time interval" (default) or "most recent only", the interval count
  is derived from the time interval, and the last-hour mode is removed.

## Impact

- **Request parameters**: `app/src/main/java/org/blitzortung/android/data/ClusterParameters.kt`
  (drop `latestOnly`-pinned range derivation; carry `intervalCount`),
  `data/provider/LocalData.kt` (`clusterParameters(...)` gains the resolved count),
  `data/MainDataHandler.kt` (`updateClusterData`, `onSharedPreferenceChanged`,
  `CLUSTER_INTERVAL_*` constants).
- **Settings/resources**: `res/xml/preferences.xml` (default value),
  `res/values/strings.xml` (`cluster_intervals` / `cluster_interval_values` arrays),
  `app/view/PreferenceKey.kt` (no key change, value semantics change).
- **Tests**: `data/ClusterParametersTest.kt`, `data/provider/standard/JsonRpcDataProviderTest.kt`,
  `data/MainDataHandlerTest.kt`.
- **Specs**: `openspec/specs/cluster-overlay/spec.md`.
- **No change**: `ClusterOverlay`, `LegendView`, the RPC endpoint contract, the strike/grid
  path, and the cluster cache behaviour. No new dependency.
