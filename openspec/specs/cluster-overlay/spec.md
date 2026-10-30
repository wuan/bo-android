## Purpose

Fetch cluster data from the RPC backend (global/local, with a 10-minute bin window
that follows the selected time interval or the most recent interval only), expose it
through a reactive stream, and render it as a static outline overlay that can be shown
simultaneously with the point/grid view.

## Requirements

### Requirement: Cluster data retrieval from RPC backend
The app SHALL retrieve cluster data from the RPC backend using `get_global_clusters` when in global mode and `get_local_clusters` when in local mode, mirroring the grid data global/local pattern. Each returned cluster SHALL be represented with its timestamp, interval duration, strike count, area, and polygon outline (shape) as a list of longitude/latitude pairs.

#### Scenario: Global cluster request
- **WHEN** cluster display is enabled and the map is at a wide (global) zoom level
- **THEN** the app requests clusters via `get_global_clusters` with the configured cluster interval and interval count

#### Scenario: Local cluster request
- **WHEN** cluster display is enabled and the map is zoomed in to a local area
- **THEN** the app requests clusters via `get_local_clusters` using the tile coordinates (`x`, `y`) and `data_area` derived from the same map tile computation used for local grid data

#### Scenario: Cluster response parsed
- **WHEN** the backend returns a cluster response
- **THEN** each element of `clusters` is parsed into a cluster with its `timestamp`, `interval_seconds`, `strike_count`, `area`, and a `shape` polygon of `[longitude, latitude]` pairs

### Requirement: Independent cluster event stream
The app SHALL deliver cluster data through a cluster-specific stream that is independent of the strike data stream, so cluster fetching can be enabled, disabled, or reconfigured without refetching strike data.

#### Scenario: Cluster data broadcast
- **WHEN** a cluster fetch completes successfully
- **THEN** a cluster event is emitted on the cluster channel and delivered to registered cluster consumers

#### Scenario: Strike stream unaffected
- **WHEN** cluster display is toggled on or off
- **THEN** no strike or grid data request is triggered solely by the toggle

### Requirement: Cluster overlay rendering
The app SHALL render clusters as static polygon outlines on the map, colored by cluster age, and SHALL allow the overlay to be shown simultaneously with the existing strike or grid view.

#### Scenario: Clusters shown with strikes
- **WHEN** cluster display and the strike/grid view are both enabled
- **THEN** the map shows both the cluster outlines and the strike/grid data at the same time

#### Scenario: Outline-only rendering
- **WHEN** a cluster is rendered
- **THEN** its `shape` polygon is drawn as an outline without fill, with a color derived from its age

#### Scenario: Overlay cleared
- **WHEN** cluster display is toggled off or cluster data is cleared
- **THEN** the cluster overlay removes all rendered clusters from the map

### Requirement: Cluster feature restricted to supported data source
The app SHALL only offer and request cluster data when the active data provider supports clusters. The HTTP data source SHALL NOT issue cluster requests.

#### Scenario: RPC source
- **WHEN** the active data source is the RPC provider
- **THEN** the cluster preference is available and cluster requests are issued when enabled

#### Scenario: Unsupported source
- **WHEN** the active data source does not support clusters (HTTP provider)
- **THEN** the cluster preference is hidden or disabled and no cluster request is issued

### Requirement: Cluster preference gating

The app SHALL provide a preference to enable or disable cluster display, defaulting to
disabled, and SHALL provide a preference to select the cluster interval, offering either
the current time interval or the most recent interval only.

#### Scenario: Default disabled
- **WHEN** the app runs with default preferences
- **THEN** cluster display is disabled and no cluster requests are issued

#### Scenario: Enable from settings
- **WHEN** the user enables cluster display in settings
- **THEN** the app begins fetching and rendering clusters

### Requirement: Grid and cluster display selection in quick settings
The app SHALL provide, in the quick settings dialog, a control to enable or disable the cluster overlay and, only while clusters are enabled, an additional control to hide the grid/strike overlay. Hiding the grid SHALL only be available when clusters are shown, so that the map can never end up with both overlays disabled. The strike count threshold control SHALL be removed from the quick settings dialog. The selected display toggles SHALL be persisted as boolean preferences (`SHOW_CLUSTERS`, `HIDE_GRID`).

#### Scenario: Grid hidden only with clusters
- **WHEN** the user enables clusters, hides the grid and confirms
- **THEN** `SHOW_CLUSTERS` is true and `HIDE_GRID` is true, and the map shows clusters without the grid/strike overlay

#### Scenario: Hide grid unavailable without clusters
- **WHEN** clusters are disabled
- **THEN** the hide-grid control is disabled, unchecked, and the grid/strike overlay is shown

#### Scenario: Empty map impossible
- **WHEN** clusters are disabled, or the active data source does not support clusters
- **THEN** the grid/strike overlay is shown regardless of `HIDE_GRID`

#### Scenario: Count threshold removed
- **WHEN** the quick settings dialog is shown
- **THEN** it contains no strike count threshold control

#### Scenario: Toggle persistence
- **WHEN** the quick settings dialog is reopened
- **THEN** the cluster and hide-grid toggles reflect the persisted `SHOW_CLUSTERS`/`HIDE_GRID` values

### Requirement: Grid overlay can be hidden without stopping strike data
The app SHALL allow the grid/strike overlay to be hidden (while clusters are shown) while strike data continues to be fetched, so that alerts, the histogram and the time slider keep receiving strike data. Hiding the grid SHALL NOT change the data request parameters or stop strike retrieval.

#### Scenario: Hide grid overlay
- **WHEN** `HIDE_GRID` is enabled while clusters are shown
- **THEN** the strike/grid overlay is no longer drawn on the map, while the map fade (which provides the dark/bright map appearance) remains active

#### Scenario: Restore grid overlay
- **WHEN** `HIDE_GRID` is disabled
- **THEN** the currently held strike data is drawn again without requiring a refetch

#### Scenario: Strike data continues while grid hidden
- **WHEN** the grid overlay is hidden
- **THEN** strike data requests continue and consumers such as alerts and the histogram still receive data

### Requirement: Cluster cache with short-lived entries
The app SHALL cache cluster responses separately from strike data using an expiry shorter than the strike cache TTL, reflecting that clusters are produced every minute.

#### Scenario: Cluster cache hit
- **WHEN** an identical cluster request is made within the cluster cache expiry window
- **THEN** the cached cluster response is used instead of issuing a new request

#### Scenario: Cluster cache expiry
- **WHEN** an identical cluster request is made after the cluster cache expiry window has elapsed
- **THEN** a new cluster request is issued

### Requirement: Cluster interval follows the selected time interval

The app SHALL keep the cluster bin length fixed at 10 minutes and SHALL derive the
number of requested intervals from the current time interval. In "Match time interval"
mode (the default) a request SHALL use `interval_count = timeInterval / 10`; in
"Most recent only" mode it SHALL use `interval_count = 1`. History and animation SHALL
continue to be expressed through `minute_offset`.

#### Scenario: Default matches the time interval
- **WHEN** cluster display is enabled and the cluster interval preference is "Match time interval" and the time interval is 60 minutes
- **THEN** the app requests clusters with `minute_length = 10` and an interval count of 6

#### Scenario: Match scales with the time interval
- **WHEN** the cluster interval preference is "Match time interval" and the time interval is 30, 90, 120 or 240 minutes
- **THEN** the app requests clusters with `minute_length = 10` and an interval count of 3, 9, 12 or 24 respectively

#### Scenario: Default cluster interval preference
- **WHEN** no cluster interval preference has been set
- **THEN** the app uses the "Match time interval" mode

#### Scenario: Legacy value tolerated
- **WHEN** the stored cluster interval preference holds the legacy value `hour`
- **THEN** the app treats it as "Match time interval"

#### Scenario: Most recent only
- **WHEN** the cluster interval preference is set to "Most recent only"
- **THEN** the app requests clusters with `minute_length = 10` and an interval count of 1

#### Scenario: History offset applied
- **WHEN** the user navigates to a past interval
- **THEN** the cluster request includes the corresponding negative `minute_offset`

#### Scenario: Animation playback
- **WHEN** animation mode is running and cluster display is enabled
- **THEN** a cluster fetch is issued for each animation step using the stepped `minute_offset`, and the rendered clusters follow the animation

### Requirement: Cluster age coloring spans the requested window

The app SHALL color cluster outlines over a time span equal to the cluster request
window, so that when clusters are matched to the time interval the cluster color scale
equals the strike color scale for that time interval.

#### Scenario: Cluster colors align with the strike legend
- **WHEN** the cluster interval preference is "Match time interval" and the time interval is 120 minutes
- **THEN** cluster outlines are colored over a 120-minute span, the same scale as the strike legend

#### Scenario: Most recent only uses its single-bin span
- **WHEN** the cluster interval preference is "Most recent only"
- **THEN** cluster outlines are colored over a single 10-minute span
