# Spec Delta

## ADDED Requirements

### Requirement: Car map shows the data timestamp

The car map SHALL show the timestamp of the current strike data on the map
surface as `Data HH:MM:SS`, using the reference time of the latest successful
data result in fixed 24-hour, zero-padded form. The timestamp SHALL be drawn by
the app in the same font as the legend labels, so it can update without
rebuilding the host template. It SHALL NOT be a separate list row, SHALL NOT
repeat the app name, and SHALL be omitted until a successful result has been
received.

#### Scenario: Data timestamp shown after a successful result

- **WHEN** the latest successful data result has reference time 12:34:56
- **THEN** the map status line SHALL show "Data 12:34:56"

#### Scenario: Timestamp follows the latest successful result

- **WHEN** a newer successful data result arrives with a different reference time
- **THEN** the displayed timestamp SHALL show the new reference time

#### Scenario: Failed result keeps the last timestamp

- **WHEN** a data request fails after a successful result
- **THEN** the displayed timestamp SHALL remain the reference time of the last successful result

#### Scenario: No timestamp before the first result

- **WHEN** no successful data result has been received
- **THEN** the status line SHALL be empty

#### Scenario: Status line updates without host template rebuild

- **WHEN** only the data timestamp changes
- **THEN** the app SHALL update the status line on the map surface without invalidating the car template

#### Scenario: Status line is not a list row

- **WHEN** the data timestamp is shown on the map surface
- **THEN** the first row of the alert list SHALL be an alert row, with no timestamp row before the alerts

## MODIFIED Requirements

### Requirement: Car map legend explains strike age colours

The car experience SHALL draw a persistent legend on the map surface that maps
the strike colours to their age, so the driver can read the colours used for
strikes. The legend SHALL be one continuous vertical band with the newest age at
the top and the oldest at the bottom, showing a smooth, unbroken colour
transition built from the active strike palette. The legend SHALL be anchored to
the bottom-left of the host's map area. It SHALL label exactly three ages — top
`0 min`, middle at half the interval, and bottom at the full interval — and SHALL
NOT draw separator lines across the band. The legend SHALL show only the age
colour scale — not the region, grid size, or strike count threshold — SHALL be
drawn above the strike, grid, and dimming overlays, and SHALL NOT be interactive.
The legend SHALL be shown without any user toggle.

#### Scenario: Legend shown with the map

- **WHEN** the map surface is available
- **THEN** the map surface SHALL show the age colour band at the bottom-left of the host's map area simultaneously with the strikes and the alert list

#### Scenario: Legend entries match the strike colours

- **WHEN** the active colour scale has N colours over a time interval
- **THEN** the legend band SHALL use those colours in the same newest-to-oldest order as the strikes

#### Scenario: Continuous band from newest to oldest

- **WHEN** the active interval is 60 minutes
- **THEN** the legend SHALL draw a smooth colour band from white at the top to the oldest colour at the bottom

#### Scenario: Three age labels

- **WHEN** the active interval is 60 minutes
- **THEN** the legend SHALL label the band "0 min" at the top, "−30 min" at the middle, and "−60 min" at the bottom

#### Scenario: Labels follow the active interval

- **WHEN** the active interval is 30 minutes
- **THEN** the legend SHALL label the band "0 min" at the top, "−15 min" in the middle, and "−30 min" at the bottom

#### Scenario: No separator lines interrupt the band

- **WHEN** the active palette has six colours over a 60-minute interval
- **THEN** the legend SHALL draw one unbroken gradient with no separator lines

#### Scenario: Legend follows the active scale

- **WHEN** the time interval or the colour scheme changes
- **THEN** the legend SHALL redraw with the new colours and age labels

#### Scenario: Legend is legible while the map is dimmed

- **WHEN** map dimming is active
- **THEN** the legend SHALL remain fully legible and SHALL NOT be dimmed

#### Scenario: Legend is not interactive

- **WHEN** the driver taps the legend area
- **THEN** the tap SHALL NOT be handled by the legend and SHALL NOT change the legend

## REMOVED Requirements

### Requirement: Car map reports the time to the next refresh

**Reason**: The countdown describes the refresh timer rather than the displayed data, forces a map redraw every second, and is replaced by the data timestamp.

**Migration**: No user action is required. The status line above the legend now shows the reference time of the latest successful data result as `Data HH:MM:SS`.
