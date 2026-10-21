## Purpose

Bounds the connect and read timeouts of outbound lightning-data HTTP requests so
that an unresponsive backend server fails fast, allowing the client to reach an
alternative resolved server address or the next scheduled update instead of
blocking for tens of seconds.

## ADDED Requirements

### Requirement: Outbound data requests use bounded connect and read timeouts

Every outbound HTTP request that retrieves lightning data SHALL set both a connect
timeout and a read timeout. The connect timeout MUST be at most 5 seconds and the
read timeout MUST be at most 10 seconds, replacing the previous 40-second values.

#### Scenario: JSON-RPC request timeout is bounded
- **WHEN** the JSON-RPC data provider issues a request to the configured service URL
- **THEN** the underlying HTTP connection is configured with a connect timeout of at most 5 seconds and a read timeout of at most 10 seconds

#### Scenario: Blitzortung HTTP request timeout is bounded
- **WHEN** the Blitzortung HTTP data provider opens a connection to retrieve strikes
- **THEN** the connection is configured with a connect timeout of at most 5 seconds and a read timeout of at most 10 seconds

### Requirement: Timeouts have a single shared definition

The connect and read timeout values SHALL be declared once as named constants in a
shared networking declaration, and every data provider MUST use those declarations
rather than repeating numeric literals.

#### Scenario: No duplicated timeout literals
- **WHEN** the JSON-RPC path and the Blitzortung HTTP path configure their connections
- **THEN** both read the same shared timeout declarations

### Requirement: Timeouts are fixed compile-time constants

The connect and read timeout values SHALL be compile-time constants. They MUST NOT
be exposed as user-configurable preferences.

#### Scenario: Settings cannot change the request timeouts
- **WHEN** the user changes application settings
- **THEN** the outbound request timeouts remain at their compile-time values

### Requirement: A timed-out request fails without crashing

When a connection attempt or a read exceeds its timeout, the system MUST treat the
request as a network failure — the JSON-RPC path surfaces a data-provider error and
the Blitzortung path yields an empty result — and MUST NOT crash the app or block
indefinitely.

#### Scenario: Connect timeout
- **WHEN** no resolved server address accepts a connection within the connect timeout
- **THEN** the request is reported as a failed result and the app continues running

#### Scenario: Read timeout
- **WHEN** a server accepts the connection but sends no response within the read timeout
- **THEN** the request is reported as a failed result and the app continues running

### Requirement: Failed requests give way to alternative servers

The system SHALL rely on DNS-based multi-record round-robin for the service host.
When a connection attempt to one resolved record does not succeed within the connect
timeout, the system SHALL take the next resolved record within the same request, and
SHALL retry on the next scheduled update when the request ultimately fails, so that
a short connect timeout yields faster fallback to an alternative server.

#### Scenario: DNS round-robin records
- **WHEN** the service host resolves to multiple DNS records and a connection attempt to one record does not succeed within the connect timeout
- **THEN** the client takes the next resolved record within the same request

#### Scenario: Next update retries after a total failure
- **WHEN** all connection attempts for a request fail or time out
- **THEN** the request is reported as a failed result and the next scheduled update retries the request
