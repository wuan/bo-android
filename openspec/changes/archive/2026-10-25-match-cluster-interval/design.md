# Design

## Context

See `proposal.md` for motivation. The relevant current state:

- `MainDataHandler.updateClusterData()` builds the request as
  `localData.clusterParameters(ClusterParameters.DEFAULT_MINUTE_LENGTH)` then
  `.withMinuteOffset(parameters.intervalOffset).withLatestOnly(clustersLatestOnly)`.
  It never reads `parameters.intervalDuration`.
- `ClusterParameters` derives `intervalCount` from a fixed `CLUSTER_RANGE_MINUTES = 60`
  via `intervalCountFor(minuteLength)`, so the window is pinned to 60 minutes ("hour") or
  10 minutes ("latest"). `minuteLength` is always `10` in practice.
- The RPC contract is `get_global_clusters(minute_length, minute_offset, interval_count)`
  / `get_local_clusters(x, y, minute_length, minute_offset, data_area, interval_count)`.
  The server produces cluster bins of `minute_length`; the response `dt` is
  `minute_length * 60`, and each `interval_count` requests one more consecutive bin.
- `ClusterOverlay.setClusters()` colors each outline over
  `resultIntervalDuration * resultParameters.intervalCount`, i.e. the window it actually
  fetched. `LegendView` colors the strike legend over `parameters.intervalDuration`.
- `CLUSTER_INTERVAL` is a `ListPreference` with values `latest` / `hour`
  (`defaultValue="latest"`); `MainDataHandler` maps it to the `clustersLatestOnly` flag.

The fixed knob is therefore `interval_count`, not `minute_length`.

## Goals / Non-Goals

**Goals:**
- Make the cluster request window equal the selected time interval in "Match" mode, while
  keeping the 10-minute cluster bin length untouched.
- Align the cluster age-color span with the strike legend for every time interval without
  changing the overlay or legend code.
- Resolve the preference/value model so upgraded installs and restored backups keep
  working.

**Non-Goals:**
- Changing the cluster bin length (`minute_length`) or the RPC endpoints.
- Server-side vertex reduction / level-of-detail for large payloads.
- Touching the strike/grid request path, the strike legend, or the cluster cache TTL.

## Decisions

### D1: Vary `interval_count`; keep `minute_length = 10`

The server's cluster bins are 10 minutes; `minute_length` is not a display-tunable, unlike
the strike raster which can change its bin size in time. So the only way to match the time
interval is `interval_count = timeInterval / 10`.

- **Why**: changing `minute_length` would change the granularity of the returned clusters
  and is not a supported tuning axis.
- **Alternatives considered**: (A) `minute_length = timeInterval, interval_count = 1` —
  rejected: it discards the per-bin age gradient and presumes the backend aggregates to an
  arbitrary bin length; (B) leave the window fixed and re-scale only the coloring —
  rejected: the outlines would still represent the wrong time range.

### D2: Two preference modes — "Match time interval" (default) and "Most recent only"

Drop "Last hour"; with the default 60-minute time interval, "Match" already equals the old
last-hour behavior, and "Match" generalizes it to 30/90/120/240.

- **Why**: one mode that tracks the time interval replaces two fixed modes and removes the
  legend drift.
- **Alternatives considered**: keep all three — rejected as redundant ("Last hour" is
  exactly "Match" at 60). Remove the preference entirely — rejected: "Most recent only"
  (a single fresh bin) is still a useful distinct view.
- **Reversal note**: this reverses archived decision D6 of `add-cluster-overlay`, which
  rejected reusing `INTERVAL_DURATION` to avoid coupling. The coupling is now intentional:
  it is what keeps the cluster and strike color scales equal.

### D3: Carry the resolved `intervalCount` on `ClusterParameters`

Replace the `latestOnly` + `CLUSTER_RANGE_MINUTES` derivation with an explicit
`intervalCount`. `MainDataHandler` resolves it (`latest ? 1 : intervalDuration / 10`) and
attaches it to the parameters built from `LocalData`; `LocalData` stays responsible only
for the global/local choice and `dataArea`.

- **Why**: the RPC call takes `interval_count` directly; carrying the resolved value
  removes a UI concept (`latestOnly`) from the request type and drops the hardcoded range.
- **Alternatives considered**: keep `latestOnly` and add `rangeMinutes`, deriving in the
  type — rejected: keeps the special case and a second derivation path for no benefit.

### D4: Preference values `match` / `latest`, legacy `hour` tolerated

The `cluster_interval` entries become "Match time interval" (`match`, default) and
"Most recent only" (`latest`). Reading uses `match` as the fallback, so a stored legacy
`hour` resolves to `match` without a migration step.

- **Why**: `PreferencesBackup` re-applies strings by type only, so a restored `hour` can
  reach the reader; defaulting the read keeps behavior defined.
- **Alternatives considered**: one-time rewrite of `hour` → `match` — deferred; the only
  visible gap is the `ListPreference` summary showing blank for a stale value until the
  user opens the picker, which is cosmetic.

### D5: No overlay or legend changes

`ClusterOverlay` already sizes its color span from `interval_count`; once the window equals
the time interval, the spans coincide. `LegendView` is unchanged.

- **Why**: the alignment falls out of the request parameter, keeping the change small and
  the rendering path untouched.

## Risks / Trade-offs

- **Payload growth** → the worst case moves from 6 to 24 global bins (240-minute time
  interval), and the archived spike could not measure global cluster vertex counts.
  Mitigation: keep the change request-parameter-only, so an `interval_count` cap can be
  added later without touching specs; flag for a live payload check before release.
- **Non-multiple-of-10 time interval** → `intervalDuration / 10` truncates silently if the
  duration list ever gains a value like 45. Mitigation: integer-divide against the existing
  `30/60/90/120/240` set (all divisible); add a guard/constant so the assumption is visible.
- **Behavioral default change** → installs that never set the cluster interval move from a
  10-minute window to the (default 60-minute) time-interval window. Mitigation: documented
  as a breaking behavioral change in the proposal; no data migration needed.
- **Legacy `hour` display** → the settings summary can render blank for a stale value.
  Mitigation: accepted as cosmetic; see D4.

## Migration Plan

- No persistent data migration: the reader treats any non-`latest` value, including the
  legacy `hour`, as `match`.
- `PreferenceManager.setDefaultValues` picks up the new `defaultValue="match"` for fresh
  installs; `PreferencesBackup` reset/import continues to work (string type unchanged).
- Rollback is limited to reverting the change; the strike/grid path and the RPC contract
  are unaffected.

## Open Questions

- The backend's maximum accepted `interval_count` (whether 24 is honored or clamped) —
  deferrable: it affects payload size, not the chosen approach; verify with a live probe.
