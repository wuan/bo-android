# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
