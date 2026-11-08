# Spec Delta

## Purpose

Defines how a requested ground distance becomes a map zoom, and the own-location
framing distance shared by the phone and the car.

## ADDED Requirements

### Requirement: Map zoom is an exact Mercator fit

The app SHALL compute a framing zoom that fits a requested ground distance across
the shorter side of the map viewport, using the Web-Mercator scale at the target
latitude. The computation SHALL produce a fractional zoom and SHALL NOT apply an
approximate divisor or offset that frames a different distance.

#### Scenario: Phone own-location framing

- **WHEN** the phone's own-location action frames a target distance
- **THEN** the map zoom SHALL be the exact fit for that distance, the shorter viewport side, and the target latitude

#### Scenario: Phone histogram framing

- **WHEN** the phone's histogram action frames a region
- **THEN** the map zoom SHALL be the exact fit for the requested ground span

#### Scenario: Car location framing

- **WHEN** the car's location action frames a target distance
- **THEN** the camera target zoom SHALL be the exact fit for the map surface's shorter side and the fix latitude

#### Scenario: Higher latitude uses the Mercator scale

- **WHEN** the same ground distance is framed at a higher latitude
- **THEN** the target zoom SHALL account for the Mercator scale so the ground distance still fits exactly

#### Scenario: Fractional zoom is not rounded

- **WHEN** the exact fit falls between two whole zoom levels
- **THEN** the target zoom SHALL remain fractional

### Requirement: Histogram tap zooms out in stages

When a phone histogram tap frames a non-global data area, a following tap at
that framing SHALL zoom out to the global view instead of re-framing the same
data area. A tap while the map is zoomed in tighter than the data-area framing
SHALL first zoom out to the data area. Global results SHALL go directly to the
global view.

#### Scenario: Data area first

- **WHEN** the histogram is tapped while the map is wider than the data area
- **THEN** the map SHALL frame the data area

#### Scenario: Global view second

- **WHEN** the histogram is tapped while the map already frames the data area
- **THEN** the map SHALL zoom out to the global view

#### Scenario: Zoomed in within the data area

- **WHEN** the histogram is tapped while the map is zoomed in tighter than the data-area framing
- **THEN** the map SHALL first zoom out to the data area

#### Scenario: Global result

- **WHEN** the histogram is tapped for a global result
- **THEN** the map SHALL frame the global view

### Requirement: Framing distances honor the measurement system

The app SHALL interpret alert range steps and strike distances in the active
measurement system - kilometres for metric and miles for imperial - and SHALL
convert them to metres before computing a framing zoom.

#### Scenario: Metric distance converted from kilometres

- **WHEN** the measurement system is metric and the framing distance is 500
- **THEN** the fit SHALL use 500,000 metres

#### Scenario: Imperial distance converted from miles

- **WHEN** the measurement system is imperial and the framing distance is 500
- **THEN** the fit SHALL use 804,672 metres

#### Scenario: Unit change changes the framed ground area

- **WHEN** the same numeric framing distance is used under metric and imperial
- **THEN** the imperial fit SHALL frame the larger ground distance

### Requirement: Own-location framing targets the alert area

The phone and car SHALL frame own-location views with one target-distance policy:
the outermost alert range step, reduced to twice the closest known strike
distance when alert activity reports one, bounded below by 50 distance units and
above by the outermost step. Doubling the distance keeps activity visible in
both portrait and landscape, where the shorter viewport side limits the fit.

#### Scenario: No closest strike is known

- **WHEN** the alert state reports no closest strike
- **THEN** the target distance SHALL be the outermost alert range step

#### Scenario: Closest strike reduces the target

- **WHEN** the closest strike distance is 100 distance units
- **THEN** the target distance SHALL be 200 distance units

#### Scenario: Target is bounded below

- **WHEN** twice the closest strike distance is below 50 distance units
- **THEN** the target distance SHALL be 50 distance units

#### Scenario: Target is bounded above

- **WHEN** twice the closest strike distance exceeds the outermost alert range step
- **THEN** the target distance SHALL be the outermost alert range step

#### Scenario: Activity without a closest sector

- **WHEN** alert activity is reported but no sector has a finite closest strike
- **THEN** the target distance SHALL be the outermost alert range step

#### Scenario: Phone keeps its zoom when configured

- **WHEN** the phone's keep-zoom-on-goto-own-location preference is enabled
- **THEN** the phone SHALL keep its current zoom instead of applying the target distance

#### Scenario: Car always applies the target policy

- **WHEN** the driver uses the car's location action
- **THEN** the car SHALL apply the target-distance policy with no keep-zoom option
