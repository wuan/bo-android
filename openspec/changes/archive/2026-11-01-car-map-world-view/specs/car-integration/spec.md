# Spec Delta

## MODIFIED Requirements

### Requirement: Car map shows live lightning strikes

The system SHALL render lightning strikes on the in-car map surface, colored by
strike age, and SHALL update the rendering as new strike data and location
changes arrive. On the first available location fix the map SHALL frame the
current position by centering on it at a default zoom, and SHALL preserve the
user's pan or zoom after that initial framing.

#### Scenario: Strikes rendered by age

- **WHEN** the map surface is available and strike data exists
- **THEN** each strike SHALL be drawn at its geographic position with a color derived from its age

#### Scenario: Map refreshes on new data

- **WHEN** new strike data is received while the map is shown
- **THEN** the map SHALL redraw to include the new strikes and drop expired ones

#### Scenario: Map follows the user location

- **WHEN** the device location changes or becomes available
- **THEN** the map SHALL indicate the current position relative to the rendered strikes

#### Scenario: Map starts framed on the current position

- **WHEN** the map surface becomes available, no camera has been stored, and a location fix arrives before the user has panned or zoomed
- **THEN** the map SHALL centre on the current position at the default zoom

#### Scenario: Initial framing happens once

- **WHEN** the map has already framed the current position and a later location update arrives
- **THEN** the map SHALL update the own-position marker only and SHALL NOT change its centre or zoom

#### Scenario: User navigation is preserved

- **WHEN** the user pans or zooms the map before the first location fix arrives
- **THEN** the map SHALL NOT recentre or change zoom on that first fix

#### Scenario: No location available

- **WHEN** no location fix is available, for example because access was denied
- **THEN** the map SHALL NOT change its centre or zoom in response to the missing location, and SHALL show the stored camera when one exists, or the world view otherwise

## ADDED Requirements

### Requirement: Car map falls back to a world view without a location

When the car map has no location fix and no stored camera, it SHALL show a world
view: the whole world covering the viewport with no empty margins, centred on 0°
latitude and 0° longitude. The world view zoom SHALL be derived from the current
map surface dimensions so that it fits the surface in use.

#### Scenario: World view on a fresh map without a location

- **WHEN** the map surface becomes available, no camera has been stored, and no location fix is available
- **THEN** the map SHALL show the whole world covering the viewport with no empty margins, centred on 0° latitude and 0° longitude

#### Scenario: World view fits the surface in use

- **WHEN** the map surface becomes available with different dimensions than before
- **THEN** the world view zoom SHALL be recomputed so that the world still covers the new viewport

#### Scenario: World view does not wait for a location event

- **WHEN** the map surface becomes available with no stored camera, even if no location event has been received
- **THEN** the map SHALL already show the world view

### Requirement: Car map bounds the visible world

The car map SHALL NOT show the world smaller than the viewport: the world view
zoom SHALL be the minimum zoom level for the session. The car map SHALL NOT allow
panning outside the world's bounds.

#### Scenario: Zoom-out stops at the world view

- **WHEN** the driver zooms out beyond the world view zoom
- **THEN** the zoom SHALL stop at the world view zoom and the world SHALL NOT appear smaller than the viewport

#### Scenario: Panning is clamped to the world

- **WHEN** the driver pans beyond the edge of the world
- **THEN** the map SHALL stop at the world's bounds and SHALL NOT scroll past them

#### Scenario: Restored camera is clamped to the floor

- **WHEN** a stored camera is restored with a zoom below the world view zoom for the current surface
- **THEN** the restored zoom SHALL be raised to the world view zoom

### Requirement: Car camera persists across sessions

The car map SHALL store its centre and zoom and restore them on the next
session, in preference to the world view, so the driver returns to their last
view. A restored camera SHALL be treated as an established view and SHALL
suppress first-fix framing.

#### Scenario: Camera restored on return

- **WHEN** the driver has panned or zoomed the map and later returns to the car app
- **THEN** the map SHALL restore the stored centre and zoom instead of the world view

#### Scenario: Restored camera suppresses first-fix framing

- **WHEN** a stored camera is restored and a location fix then arrives
- **THEN** the map SHALL keep the restored centre and zoom and SHALL NOT reframe on the current position

#### Scenario: Camera saved on navigation

- **WHEN** the driver pans or zooms the map
- **THEN** the new centre and zoom SHALL be stored for the next session

#### Scenario: World view only without a stored camera

- **WHEN** no camera has been stored, including on the first ever run
- **THEN** the map SHALL use the world view rather than a stored camera
