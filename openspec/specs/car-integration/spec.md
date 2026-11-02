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

### Requirement: Car alert list reports the time to the next refresh

While the data handler is in realtime mode and reports its periodic refresh
status, the car map header SHALL show a compact countdown to the next data
refresh on the same line as the app name, in the form `<remaining> / <period>s`.
The countdown SHALL NOT be a separate list row, and SHALL be omitted before a
refresh status has been reported.

#### Scenario: Countdown shown in realtime mode

- **WHEN** the data handler reports its periodic refresh status in realtime mode
- **THEN** the map header SHALL show the compact countdown on the same line as the app name

#### Scenario: Compact countdown format

- **WHEN** the reported status is 40 seconds remaining of a 60-second period
- **THEN** the header SHALL show "40 / 60s"

#### Scenario: Countdown follows the reported status

- **WHEN** the reported refresh status changes
- **THEN** the displayed countdown SHALL follow the new remaining and period values

#### Scenario: No separate countdown row

- **WHEN** the countdown is shown in the header
- **THEN** the first row of the alert list SHALL be an alert row, with no countdown row between the header and the alerts

#### Scenario: No countdown before a status is reported

- **WHEN** no refresh status has been reported
- **THEN** the header SHALL show only the app name

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
the strike colours to their age in minutes, so the driver can read the colours
used for strikes. The legend SHALL show only the age colour scale — not the
region, grid size, or strike count threshold — SHALL be drawn above the strike,
grid, and dimming overlays, and SHALL NOT be interactive. The legend SHALL be
shown without any user toggle. The legend SHALL be drawn in the top-left of the
map surface, below the host's status card, so it stays clear of that card even as
that card grows.

#### Scenario: Legend shown with the map

- **WHEN** the map surface is available
- **THEN** the map surface SHALL show the age colour legend in the top-left of the map surface, below the host's status card, simultaneously with the strikes and the alert list

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
