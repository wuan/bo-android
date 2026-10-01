## ADDED Requirements

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

### Requirement: Configurable cluster interval over the last hour
The app SHALL fetch clusters for a configurable interval of 5 or 10 minutes covering the last hour, and SHALL support history via a minute offset. A single request SHALL use `interval_count = 60 / interval` so one response covers the whole hour.

#### Scenario: Default interval
- **WHEN** cluster display is enabled and no cluster interval preference has been set
- **THEN** the app requests clusters with a 10-minute interval and an interval count of 6

#### Scenario: Five minute interval
- **WHEN** the cluster interval preference is set to 5 minutes
- **THEN** the app requests clusters with `minute_length = 5` and an interval count of 12

#### Scenario: History offset applied
- **WHEN** the user navigates to a past interval
- **THEN** the cluster request includes the corresponding negative `minute_offset`

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
The app SHALL provide a preference to enable or disable cluster display, defaulting to disabled, and SHALL provide a preference to select the cluster interval with allowed values of 5 or 10 minutes.

#### Scenario: Default disabled
- **WHEN** the app runs with default preferences
- **THEN** cluster display is disabled and no cluster requests are issued

#### Scenario: Enable from settings
- **WHEN** the user enables cluster display in settings
- **THEN** the app begins fetching and rendering clusters

### Requirement: Grid and cluster display selection in quick settings
The app SHALL provide, in the quick settings dialog, two independent controls to enable or disable the grid/strike overlay and the cluster overlay. The strike count threshold control SHALL be removed from the quick settings dialog. The selected display toggles SHALL be persisted as boolean preferences.

#### Scenario: Independent toggles
- **WHEN** the user enables clusters and disables the grid in the quick settings dialog and confirms
- **THEN** `SHOW_CLUSTERS` is true and `SHOW_GRID` is false, and the map shows clusters without the grid/strike overlay

#### Scenario: Count threshold removed
- **WHEN** the quick settings dialog is shown
- **THEN** it contains no strike count threshold control

#### Scenario: Toggle persistence
- **WHEN** the quick settings dialog is reopened
- **THEN** the grid and cluster toggles reflect the persisted `SHOW_GRID`/`SHOW_CLUSTERS` values

### Requirement: Grid overlay can be deactivated without stopping strike data
The app SHALL allow the grid/strike overlay to be hidden while strike data continues to be fetched, so that alerts, the histogram and the time slider keep receiving strike data. Disabling the grid SHALL NOT change the data request parameters or stop strike retrieval.

#### Scenario: Hide grid overlay
- **WHEN** `SHOW_GRID` is disabled
- **THEN** the strike/grid overlay is no longer drawn on the map, while the map fade (which provides the dark/bright map appearance) remains active

#### Scenario: Restore grid overlay
- **WHEN** `SHOW_GRID` is re-enabled
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
