# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Configurable cluster interval over the last hour

**Reason**: The cluster window is derived from the user's selected time interval instead
of a fixed last-hour or most-recent choice, so cluster age coloring stays consistent with
the strike legend.

**Migration**: Use the "Cluster interval follows the selected time interval" requirement.
The cluster interval preference now offers "Match time interval" (default) and
"Most recent only"; a stored legacy `hour` value is treated as "Match time interval".
