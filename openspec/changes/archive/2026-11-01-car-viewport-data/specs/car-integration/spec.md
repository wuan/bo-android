# Spec Delta

## ADDED Requirements

### Requirement: Car map data follows the map viewport

The car map SHALL derive its data grid size and its global/local data area from
the map viewport, so panning and zooming anywhere in the world always yield
appropriate data: global grid data with squares of 25 km or larger at low zoom,
and local grid data with squares smaller than 25 km when zoomed in.

#### Scenario: Wide view uses global data

- **WHEN** the driver zooms the car map out to a wide view
- **THEN** the car SHALL request global grid data with squares of 25 km or larger

#### Scenario: Zoomed-in view uses local data

- **WHEN** the driver zooms the car map in past the global/local threshold
- **THEN** the car SHALL request local grid data with squares smaller than 25 km for the viewed area

#### Scenario: Panning retargets local data

- **WHEN** the driver pans the car map to a different area while zoomed in
- **THEN** the car SHALL request local grid data for the newly viewed area

#### Scenario: Grid size follows the zoom

- **WHEN** the map zoom changes enough to cross a grid-size step
- **THEN** the car SHALL request data at the grid size for the new zoom

### Requirement: Car map has no region or grid-size configuration

The car experience SHALL always size its grid automatically and SHALL always use
the automatic global/local mode. It SHALL NOT expose region or grid-size
settings, SHALL NOT honour the phone's region or grid-size preferences, and SHALL
NOT request a fixed classical region.

#### Scenario: Settings offer no region or grid size

- **WHEN** the driver opens the car settings
- **THEN** the car SHALL NOT offer a region or grid-size setting

#### Scenario: A stored fixed region or grid size is ignored

- **WHEN** the stored region or grid-size preference holds a fixed value
- **THEN** the car SHALL still derive its grid size from the viewport and SHALL use the automatic global/local mode

#### Scenario: Only global or local data is requested

- **WHEN** the car requests strike data
- **THEN** the request SHALL use the global or the local mode and SHALL NOT name a classical region

### Requirement: Car map does not outline the local data area

The car map SHALL NOT draw the local data-area outline that the phone map draws,
so no data-area boundary is shown while local data is loaded.

#### Scenario: No outline for local data

- **WHEN** the car map is showing local grid data
- **THEN** the map SHALL NOT draw a boundary outline around the loaded data area

### Requirement: Car defers its first data request until the map viewport is known

The car SHALL NOT request strike data before the map viewport is known, so the
first request reflects the initial map view rather than a default view.

#### Scenario: No request before the map is laid out

- **WHEN** the car map screen starts and the map has not yet been laid out
- **THEN** the car SHALL NOT request strike data

#### Scenario: First request matches the framed view

- **WHEN** the map viewport becomes known
- **THEN** the car SHALL request data matching that view
