# Spec Delta

## Purpose

Deliver the app's live lightning data and proximity alerts to Android Auto and
Android Automotive OS screens as a Weather-category templated car experience,
without changing how the app behaves on phones.

## ADDED Requirements

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
changes arrive.

#### Scenario: Strikes rendered by age

- **WHEN** the map surface is available and strike data exists
- **THEN** each strike SHALL be drawn at its geographic position with a color derived from its age

#### Scenario: Map refreshes on new data

- **WHEN** new strike data is received while the map is shown
- **THEN** the map SHALL redraw to include the new strikes and drop expired ones

#### Scenario: Map follows the user location

- **WHEN** the device location changes or becomes available
- **THEN** the map SHALL indicate the current position relative to the rendered strikes

### Requirement: Car alert list summarizes proximity alerts

The system SHALL display an alert list alongside the map that reports the closest
strike's distance and bearing and the per-sector strike counts, and SHALL update
it as alert state changes.

#### Scenario: Closest strike is reported

- **WHEN** at least one strike is within the configured alert range in the alert time window
- **THEN** the alert list SHALL show the closest strike's distance and bearing

#### Scenario: Sector counts are reported

- **WHEN** strikes fall into one or more sectors
- **THEN** the alert list SHALL show the strike count for each active sector

#### Scenario: Cleared alert

- **WHEN** no strike remains within range for the alert time window
- **THEN** the alert list SHALL indicate that there is no current alert

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
