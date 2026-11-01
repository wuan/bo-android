# Spec Delta

## ADDED Requirements

### Requirement: Car map legend explains strike age colours

The car experience SHALL draw a persistent legend on the map surface that maps
the strike colours to their age in minutes, so the driver can read the colours
used for strikes. The legend SHALL show only the age colour scale — not the
region, grid size, or strike count threshold — SHALL be drawn above the strike,
grid, and dimming overlays, and SHALL NOT be interactive. The legend SHALL be
shown without any user toggle. The legend SHALL be anchored to the map area the
host reports as visible or stable — not the raw surface corner — so it is not
obscured by host UI such as the alert list.

#### Scenario: Legend shown with the map

- **WHEN** the map surface is available
- **THEN** the map surface SHALL show the age colour legend in the top-left corner of the host's visible map area, clear of host UI such as the alert list, simultaneously with the strikes and the alert list

#### Scenario: Legend entries match the strike colours

- **WHEN** the active colour scale has N colours over a time interval
- **THEN** the legend SHALL show one row per colour, each row drawn in that colour, labelled with the age it represents

#### Scenario: Legend follows the active scale

- **WHEN** the time interval or the colour scheme changes
- **THEN** the legend SHALL redraw with the new colours and age labels

#### Scenario: Legend is legible while the map is dimmed

- **WHEN** map dimming is active
- **THEN** the legend SHALL remain fully legible and SHALL NOT be dimmed

#### Scenario: Legend is not interactive

- **WHEN** the driver taps the legend area
- **THEN** the tap SHALL NOT be handled by the legend and SHALL NOT change the legend
