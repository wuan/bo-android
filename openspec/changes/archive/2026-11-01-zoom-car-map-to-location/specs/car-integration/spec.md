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

- **WHEN** the map surface becomes available and a location fix arrives before the user has panned or zoomed
- **THEN** the map SHALL centre on the current position at the default zoom

#### Scenario: Initial framing happens once

- **WHEN** the map has already framed the current position and a later location update arrives
- **THEN** the map SHALL update the own-position marker only and SHALL NOT change its centre or zoom

#### Scenario: User navigation is preserved

- **WHEN** the user pans or zooms the map before the first location fix arrives
- **THEN** the map SHALL NOT recentre or change zoom on that first fix

#### Scenario: No location available

- **WHEN** no location fix is available, for example because access was denied
- **THEN** the map SHALL leave its centre and zoom unchanged
