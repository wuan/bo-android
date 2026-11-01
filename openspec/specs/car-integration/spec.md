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
status, the car alert list SHALL display the time remaining until the next data
refresh. It SHALL NOT display a countdown before a refresh status has been
reported.

#### Scenario: Countdown shown in realtime mode

- **WHEN** the data handler reports its periodic refresh status in realtime mode
- **THEN** the alert list SHALL show the time remaining until the next refresh

#### Scenario: Countdown follows the reported status

- **WHEN** the reported refresh status changes
- **THEN** the displayed time to the next refresh SHALL follow the new value

#### Scenario: No countdown before a status is reported

- **WHEN** no refresh status has been reported
- **THEN** the alert list SHALL NOT show a refresh countdown

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
