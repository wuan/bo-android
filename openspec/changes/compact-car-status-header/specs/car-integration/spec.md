# Spec Delta

## MODIFIED Requirements

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
