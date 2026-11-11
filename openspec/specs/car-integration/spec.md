## Purpose

Deliver the app's live lightning data and proximity alerts to Android Auto and
Android Automotive OS screens as a Weather-category templated car experience,
without changing how the app behaves on phones.

## Requirements

### Requirement: Car app is discoverable on Android Auto and Android Automotive OS

The app SHALL declare itself as a Weather-category car app with template support
so that Android Auto hosts and Android Automotive OS launchers can discover and
start it, in addition to the existing phone launcher entry point.

#### Scenario: Android Auto host discovers the car app

- **WHEN** an Android Auto host enumerates apps that declare the template capability
- **THEN** the app SHALL be listed and its car service SHALL be startable

#### Scenario: Android Automotive OS launcher starts the car app

- **WHEN** the user selects the app from the Android Automotive OS launcher
- **THEN** the car entry activity SHALL start and present the start screen

#### Scenario: Phone launcher is unchanged

- **WHEN** the user launches the app on a phone or tablet
- **THEN** the existing phone activity SHALL start as before

### Requirement: Car map shows live lightning strikes

The system SHALL render lightning strikes on the in-car map surface, colored by
strike age, and SHALL update the rendering as new strike data and location
changes arrive. On the first available location fix the map SHALL frame the
current position by centering on it at a default zoom, and SHALL preserve the
user's pan or zoom after that initial framing. The map SHALL offer a location
action that re-centres on the latest fix at the default zoom.

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
- **THEN** the map SHALL centre on the current position at the default zoom

#### Scenario: Location action without a fix

- **WHEN** the driver uses the map's location action and no location fix is available
- **THEN** the map SHALL keep its current centre and zoom

### Requirement: Car alert list summarizes proximity alerts

The system SHALL display an alert list alongside the map that reports the closest
strike's distance and bearing and the per-sector strike counts, and SHALL update
it as alert state changes. The list SHALL be derived from the same alert
computation the phone uses and driven by the received strike data, so that it
shows a waiting state only while no strike data is available for alerting.

#### Scenario: Closest strike is reported

- **WHEN** at least one strike is within the configured alert range in the alert time window
- **THEN** the alert list SHALL show the closest strike's distance and bearing

#### Scenario: Sector counts are reported

- **WHEN** strikes fall into one or more sectors
- **THEN** the alert list SHALL show the strike count for each active sector

#### Scenario: Cleared alert

- **WHEN** no strike remains within range for the alert time window
- **THEN** the alert list SHALL indicate that there is no current alert

#### Scenario: Alert list reflects received strike data

- **WHEN** strike data has been received and the alert computation reports no local activity
- **THEN** the alert list SHALL show the matching alert state (for example no strikes in range, or location unknown) and SHALL NOT show the waiting state

#### Scenario: Waiting state only before data arrives

- **WHEN** no strike data has been received yet
- **THEN** the alert list SHALL show the waiting state

### Requirement: Car app reuses the existing data layer

The car experience SHALL obtain strike, location, and alert data through the same
repository and handler components used by the phone app, honoring the existing
cache and update scheduling, and SHALL NOT perform its own independent data
fetching.

#### Scenario: Data provided through existing repositories

- **WHEN** the car experience requests strike, location, or alert data
- **THEN** it SHALL receive it from the existing repositories rather than a car-specific data path

#### Scenario: Cache behavior is preserved

- **WHEN** strike data is still within the cache validity window
- **THEN** the car experience SHALL use the cached result and SHALL NOT trigger a redundant backend request

### Requirement: Car app handles location permission denial

The car experience SHALL request location access through the car context and SHALL
remain usable with reduced or absent location data when the request is denied.

#### Scenario: Location permission granted

- **WHEN** the user grants location access in the car
- **THEN** strikes SHALL be shown relative to the current position

#### Scenario: Location permission denied

- **WHEN** the user denies location access in the car
- **THEN** the car experience SHALL continue without crashing and SHALL present data using the configured default or manual location

### Requirement: Car app degrades gracefully without data

The car experience SHALL present a clear empty or error state when no strike data
is available or a fetch fails, without crashing and without presenting data as
current that is not.

#### Scenario: No data available

- **WHEN** no strike data has been received
- **THEN** the car experience SHALL show an empty-state message on the map and alert list

#### Scenario: Fetch failure

- **WHEN** a data fetch fails
- **THEN** the car experience SHALL indicate the failure and SHALL NOT present the failed result as current

### Requirement: Consistent car experience across car platforms

The car experience SHALL present the same screens and behavior on Android Auto and
Android Automotive OS, differing only in the platform entry point.

#### Scenario: Same screens on both platforms

- **WHEN** the car experience runs on Android Auto
- **THEN** it SHALL show the same map and alert list screens as on Android Automotive OS

#### Scenario: Dedicated Android Automotive OS distribution

- **WHEN** the Android Automotive OS artifact is published
- **THEN** it SHALL be released on the dedicated Android Automotive OS track rather than the mobile track

### Requirement: Phone experience is unchanged

The change SHALL NOT alter the existing phone user interface, data providers,
alert computation, preference keys, or background service behavior.

#### Scenario: Phone behavior preserved

- **WHEN** a user uses the app on a phone
- **THEN** the map, alerts, settings, widget, and background monitoring SHALL behave exactly as before

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
drawn above the strike, grid, and shading overlays, and SHALL NOT be interactive.
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

#### Scenario: Legend is legible while the map is shaded

- **WHEN** map shading is active
- **THEN** the legend SHALL remain fully legible and SHALL NOT be shaded

#### Scenario: Legend is not interactive

- **WHEN** the driver taps the legend area
- **THEN** the tap SHALL NOT be handled by the legend and SHALL NOT change the legend

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
