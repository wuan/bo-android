# Spec Delta

## MODIFIED Requirements

### Requirement: Car map shows live lightning strikes

The system SHALL render lightning strikes on the in-car map surface, colored by
strike age, and SHALL update the rendering as new strike data and location
changes arrive. On the first available location fix the map SHALL frame the
current position by centering on it at a default zoom, and SHALL preserve the
user's pan or zoom after that initial framing. The map SHALL offer a location
action that animates the camera to the latest fix at the zoom that frames the
shared own-location target distance. Pan, fling, scale, and zoom input SHALL
interrupt the transition and take effect from the camera's current position;
using the location action again SHALL retarget the transition.

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

#### Scenario: Re-centre on the current position

- **WHEN** the driver uses the map's location action and a location fix is available
- **THEN** the map SHALL animate its centre and zoom to the current position at the shared own-location target zoom

#### Scenario: Location action frames the alert area

- **WHEN** the location action runs and the alert state reports no closest strike
- **THEN** the target zoom SHALL frame the outermost alert range step

#### Scenario: Location action frames the closest strike

- **WHEN** the location action runs and the alert state reports a closest strike
- **THEN** the target zoom SHALL frame that strike with the shared margin, bounded by 50 distance units and the outermost alert range step

#### Scenario: First-fix framing keeps the default zoom

- **WHEN** the first location fix frames the map
- **THEN** the map SHALL use the default zoom and SHALL NOT apply the location-action target policy

#### Scenario: Driver interrupts the transition

- **WHEN** the driver pans, flings, scales, or uses a zoom action while the location transition is running
- **THEN** the transition SHALL stop and the camera SHALL follow the driver's input from its current position

#### Scenario: Repeated location action retargets

- **WHEN** the driver uses the location action again while a transition is running
- **THEN** the camera SHALL transition to the new target without settling at the intermediate camera

#### Scenario: Location action without a fix

- **WHEN** the driver uses the map's location action and no location fix is available
- **THEN** the map SHALL keep its current centre and zoom

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

#### Scenario: Camera saved when the transition settles

- **WHEN** the location transition completes
- **THEN** the final centre and zoom SHALL be stored instead of an intermediate camera

#### Scenario: Camera saved after an interrupted transition

- **WHEN** the driver interrupts the transition and navigates
- **THEN** the resulting camera SHALL be stored for the next session

#### Scenario: World view only without a stored camera

- **WHEN** no camera has been stored, including on the first ever run
- **THEN** the map SHALL use the world view rather than a stored camera

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

#### Scenario: Data follows the settled camera

- **WHEN** the location transition completes or is interrupted
- **THEN** the car SHALL derive and request data for the settled viewport and SHALL NOT request data for intermediate animation frames

### Requirement: Phone experience is unchanged

The change SHALL NOT alter the existing phone user interface, data providers,
alert computation, preference keys, or background service behavior, except that
the phone's own-location and histogram framing SHALL use the shared exact,
unit-aware map-zoom computation.

#### Scenario: Phone behavior preserved

- **WHEN** a user uses the app on a phone
- **THEN** the map, alerts, settings, widget, and background monitoring SHALL behave as before, except that own-location and histogram framing SHALL use the shared exact, unit-aware computation

## REMOVED Requirements

### Requirement: Car alert list summarizes proximity alerts

**Reason**: The car no longer lists per-sector strike counts; it shows one
closest-activity status line to keep the map dominant.

**Migration**: Use the "Car alert list shows a closest-activity status"
requirement.

## ADDED Requirements

### Requirement: Car alert list shows a closest-activity status

The system SHALL display a single status line beside the map that reports the
closest strike's distance and bearing when local activity is present, and
otherwise the current alert state. It SHALL update the status as alert data
changes and SHALL show a waiting state only while no strike data is available
for alerting. It SHALL NOT list per-sector strike counts.

#### Scenario: Closest strike is reported

- **WHEN** at least one strike is within the configured alert range in the alert time window
- **THEN** the status line SHALL show the closest strike's distance and bearing

#### Scenario: No per-sector rows

- **WHEN** strikes fall into one or more sectors
- **THEN** the status line SHALL still show only the closest activity and SHALL NOT list per-sector strike counts

#### Scenario: Cleared alert

- **WHEN** no strike remains within range for the alert time window
- **THEN** the status line SHALL indicate that there is no current alert

#### Scenario: Alert status reflects received strike data

- **WHEN** strike data has been received and the alert computation reports no local activity
- **THEN** the status line SHALL show the matching alert state (for example no strikes in range, or location unknown) and SHALL NOT show the waiting state

#### Scenario: Waiting state only before data arrives

- **WHEN** no strike data has been received yet
- **THEN** the status line SHALL show the waiting state

### Requirement: Car location honors the configured manual location

The car experience SHALL use the configured manual location when the stored
location mode is manual and both coordinates are valid, instead of the device
provider, so an explicit manual setting is not silently replaced by GPS. Without
a valid configured manual location it SHALL prefer GPS, then network, then
passive.

#### Scenario: Manual location configured

- **WHEN** the stored location mode is manual and both coordinates are valid
- **THEN** the car SHALL use the manual location for the map, data, and alerts

#### Scenario: Manual mode without coordinates

- **WHEN** the stored location mode is manual but the coordinates are missing or invalid
- **THEN** the car SHALL use the device provider selection instead

#### Scenario: No manual location configured

- **WHEN** no valid manual location is configured
- **THEN** the car SHALL prefer GPS, then network, then passive
