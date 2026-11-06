# Spec Delta

## REMOVED Requirements

### Requirement: Car map dimming

**Reason**: Replaced by always-on, theme-aware map shading with dark and bright
directions and no unshaded mode.

**Migration**: The unreleased car dimming setting is replaced by the map-shading
setting. Dark map replaces the former always-dim mode, and Bright map replaces the
former light/unshaded treatment.

### Requirement: Car map dimming preference

**Reason**: Replaced by the map-shading preference.

**Migration**: No released preference exists. The setting is stored under the new
map-shading key and defaults to Follow car.

### Requirement: Car settings screen

**Reason**: Replaced by the same settings behavior expressed in shading terms.

**Migration**: The screen and its navigation are unchanged; only the setting it
lists changes from map dimming to map shading.

## ADDED Requirements

### Requirement: Car map shading

The car experience SHALL always shade the map background and SHALL offer three
shading modes: Follow car, Dark map, and Bright map. Follow car SHALL apply dark
shading when the car host reports dark mode and bright shading when it reports
light mode. Dark map SHALL always apply dark shading and Bright map SHALL always
apply bright shading. There SHALL be no unshaded mode.

#### Scenario: Follow car shades dark in dark mode

- **WHEN** the shading mode is Follow car and the car host reports dark mode
- **THEN** the map background SHALL be shaded dark

#### Scenario: Follow car shades bright in light mode

- **WHEN** the shading mode is Follow car and the car host reports light mode
- **THEN** the map background SHALL be shaded bright

#### Scenario: Dark map always shades dark

- **WHEN** the shading mode is Dark map
- **THEN** the map background SHALL be shaded dark regardless of the car host's theme

#### Scenario: Bright map always shades bright

- **WHEN** the shading mode is Bright map
- **THEN** the map background SHALL be shaded bright regardless of the car host's theme

#### Scenario: No unshaded mode

- **WHEN** the driver selects any shading mode
- **THEN** the map background SHALL be shaded, never left unshaded

#### Scenario: Only the map background is shaded

- **WHEN** the map background is shaded
- **THEN** the strike and legend overlays SHALL NOT be shaded

#### Scenario: Follow car reacts to a theme change

- **WHEN** the shading mode is Follow car and the car host switches between dark and light mode
- **THEN** the map shading SHALL follow the new theme

### Requirement: Car map shading preference

The car experience SHALL default map shading to Follow car when no preference has
been stored, SHALL keep the driver's selection across sessions, and SHALL store
it independently of the phone app's preferences.

#### Scenario: Default before any selection

- **WHEN** no map-shading preference has been stored
- **THEN** the map-shading mode SHALL be Follow car

#### Scenario: Selection persists across sessions

- **WHEN** the driver selects a shading mode and later returns to the car app
- **THEN** the map SHALL use the previously selected mode

#### Scenario: Independent of the phone app

- **WHEN** the phone app's preferences change
- **THEN** the car map-shading preference SHALL be unaffected

### Requirement: Car settings surface

The car experience SHALL provide a settings screen reachable from the map that
lists the available car settings, starting with map shading, and SHALL let the
driver choose the shading mode from it.

#### Scenario: Settings reachable from the map

- **WHEN** the driver activates the settings action on the map
- **THEN** the settings screen SHALL be shown

#### Scenario: Shading offered in settings

- **WHEN** the settings screen is shown
- **THEN** it SHALL list the map-shading setting

#### Scenario: Selection updates the map

- **WHEN** the driver selects a shading mode in settings
- **THEN** the map shading SHALL reflect the new mode

#### Scenario: Returning to the map

- **WHEN** the driver leaves the settings screen
- **THEN** the map SHALL remain operational with its data updates

### Requirement: Car strike colours follow the map shading

The strike colours SHALL follow the active shading direction so strikes remain
distinguishable from the shaded background. Bright shading SHALL use a darker
strike palette than dark shading.

#### Scenario: Bright shading darkens strike colours

- **WHEN** the map background is shaded bright
- **THEN** the strikes SHALL be drawn with a darker palette than on dark shading

#### Scenario: Dark shading keeps the bright strike colours

- **WHEN** the map background is shaded dark
- **THEN** the strikes SHALL be drawn with the bright palette

#### Scenario: Strike colours update when the shading changes

- **WHEN** the active shading direction changes, including on a car theme change
- **THEN** the strikes SHALL be redrawn with the palette for the new direction

#### Scenario: Legend uses the active palette

- **WHEN** the shading direction changes
- **THEN** the legend colour swatches SHALL match the palette used for the strikes
