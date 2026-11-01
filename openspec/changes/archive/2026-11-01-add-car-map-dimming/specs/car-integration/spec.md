# Spec Delta

## ADDED Requirements

### Requirement: Car map dimming

The car experience SHALL offer three map dimming modes — Follow car, On, and Off
— and SHALL dim the map surface with a translucent overlay while dimming is
active. Follow car SHALL dim the map when the car host reports dark mode and
clear it otherwise, On SHALL always dim, and Off SHALL never dim.

#### Scenario: Follow car dims in dark mode

- **WHEN** the dimming mode is Follow car and the car host reports dark mode
- **THEN** the map surface SHALL be dimmed by the translucent overlay

#### Scenario: Follow car clears in light mode

- **WHEN** the dimming mode is Follow car and the car host reports light mode
- **THEN** the map surface SHALL NOT be dimmed

#### Scenario: On always dims

- **WHEN** the dimming mode is On
- **THEN** the map surface SHALL be dimmed regardless of the car host's theme

#### Scenario: Off never dims

- **WHEN** the dimming mode is Off
- **THEN** the map surface SHALL NOT be dimmed regardless of the car host's theme

#### Scenario: Only the map background is dimmed

- **WHEN** the map is dimmed
- **THEN** the strike and grid overlays SHALL NOT be dimmed

#### Scenario: Follow car reacts to a theme change

- **WHEN** the dimming mode is Follow car and the car host switches between dark and light mode
- **THEN** the map dimming SHALL follow the new theme

### Requirement: Car map dimming preference

The car experience SHALL default map dimming to Follow car when no preference has
been stored, SHALL keep the driver's selection across sessions, and SHALL store
it independently of the phone app's preferences.

#### Scenario: Default before any selection

- **WHEN** no map-dimming preference has been stored
- **THEN** the map-dimming mode SHALL be Follow car

#### Scenario: Selection persists across sessions

- **WHEN** the driver selects a dimming mode and later returns to the car app
- **THEN** the map SHALL use the previously selected mode

#### Scenario: Independent of the phone app

- **WHEN** the phone app's preferences change
- **THEN** the car map-dimming preference SHALL be unaffected

### Requirement: Car settings screen

The car experience SHALL provide a settings screen reachable from the map that
lists the available car settings, starting with map dimming, and SHALL let the
driver choose the map-dimming mode from it.

#### Scenario: Settings reachable from the map

- **WHEN** the driver activates the settings action on the map
- **THEN** the settings screen SHALL be shown

#### Scenario: Dimming offered in settings

- **WHEN** the settings screen is shown
- **THEN** it SHALL list the map-dimming setting

#### Scenario: Selection updates the map

- **WHEN** the driver selects a dimming mode in settings
- **THEN** the map dimming SHALL reflect the new mode

#### Scenario: Returning to the map

- **WHEN** the driver leaves the settings screen
- **THEN** the map SHALL remain operational with its data updates
