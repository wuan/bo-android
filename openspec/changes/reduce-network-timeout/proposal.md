## Why

Every outbound lightning-data request currently allows up to 40 seconds before it
fails: `JsonRpcClient` sets a 40 s connect and read timeout, and
`BlitzortungHttpDataProvider.readFromUrl()` configures 40 s directly on its
connections. The app backend `bo-service.tryb.de` resolves to more than one
address (two IPv4 and two IPv6 addresses are published for the host), so a client
can reach an alternative backend server when one is unavailable — but only after
the first connection attempt gives up. Waiting 40 seconds for an unresponsive
server delays that fallback, stalls the update cycle so the map stays stale, and
lets a foreground or background fetch outlive the 5-second background wake lock
(`ServiceDataHandler.WAKELOCK_TIMEOUT`) that is meant to cover it.

Reducing the connect and read timeouts to the seconds range makes an unresponsive
backend fail fast, so the client reaches the next resolved address within the same
request, or retries on the next scheduled update, instead of blocking for tens of
seconds during a backend outage (issue #487).

## What Changes

- Add a single shared definition of the outbound HTTP connect and read timeouts in
  the seconds range (connect at most 5 s, read at most 10 s), replacing the two
  independent `40000` literals.
- Apply the shared timeouts to the JSON-RPC path (`JsonRpcClient` /
  `HttpServiceClientDefault`), including a bounded default when the client is used
  without an explicit override.
- Apply the shared timeouts to the Blitzortung HTTP path
  (`BlitzortungHttpDataProvider.readFromUrl()`), keeping its existing behavior of
  treating `SocketException` / `SocketTimeoutException` as an empty result.
- Add unit tests that pin the shared values to the bounded range, verify the
  JSON-RPC client applies them, and verify the shared helper applies them to a
  connection.
- No change to retry scheduling, provider contracts, error handling, permissions,
  dependencies, or user-visible settings.

## Capabilities

### New Capabilities
- `network-timeouts`: bounded connect and read timeouts for outbound lightning-data HTTP requests, so an unresponsive backend server fails fast and the client can fall back to another resolved address or to the next scheduled update.

### Modified Capabilities
<!-- No existing capability spec covers outbound request timeouts. -->

## Impact

- `app/src/main/java/org/blitzortung/android/jsonrpc/JsonRpcClient.kt` — set the
  client timeouts from the shared values instead of `40000`.
- `app/src/main/java/org/blitzortung/android/jsonrpc/HttpServiceClientDefault.kt`
  — default the configurable timeouts from the shared values instead of `0`.
- `app/src/main/java/org/blitzortung/android/data/provider/blitzortung/BlitzortungHttpDataProvider.kt`
  — use the shared values in `readFromUrl()` instead of `40000`.
- New shared timeout declaration (proposed
  `org.blitzortung.android.util.NetworkTimeouts`) plus its unit test.
- Tests: `JsonRpcClientTest`, `HttpServiceClientDefaultTest`, and the new
  `NetworkTimeoutsTest`.
- No new dependencies, permissions, or preference keys; no schema or data
  migration.
