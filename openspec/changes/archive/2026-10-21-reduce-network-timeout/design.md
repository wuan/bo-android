## Context

Outbound lightning-data traffic runs through two interchangeable providers selected
by `DataProviderFactory`:

- **JSON-RPC (default)** — `JsonRpcDataProvider` → `JsonRpcClient` →
  `HttpServiceClientDefault.doRequest()`. `JsonRpcClient` currently sets
  `connectionTimeout = 40000` and `socketTimeout = 40000` in its `init` block, and
  `HttpServiceClientDefault` applies those values to the `HttpURLConnection` it
  opens. The mutable defaults on `HttpServiceClientDefault` are `0` (no timeout).
- **Blitzortung HTTP (opt-in, authenticated)** — `BlitzortungHttpDataProvider`
  opens a `URLConnection` per interval file in `readFromUrl()` and currently sets
  `connectTimeout = 40000` / `readTimeout = 40000` locally. It already converts
  `SocketException` and `SocketTimeoutException` while reading into an empty strike
  list.

The default service URL is `http://bo-service.tryb.de/`. The host is published
through DNS-based multi-record round-robin (two IPv4 and two IPv6 records observed),
so when a connection attempt fails the client takes the next resolved record within
the same request. The periodic update loop (`MainDataScheduler` / `Period`, default
60 s) re-issues requests, so the next cycle is the fallback for a request that fails
entirely. Each attempt is bounded by the connect timeout; a read timeout bounds a
connection that is accepted but never responds.

Two existing bounds interact with these timeouts:

- The background fetch acquires a wake lock for `ServiceDataHandler.WAKELOCK_TIMEOUT`
  (5000 ms). A 40-second request can outlive the wake lock.
- `FetchDataTask` catches provider `RuntimeException`s and converts them into a
  failed `DataReceived` result, so a fast timeout degrades cleanly.

## Goals / Non-Goals

**Goals:**
- Bound every outbound lightning-data request with a connect and read timeout in
  the seconds range.
- Define the timeout values once and reuse them in both providers.
- Keep the change small and behavior-preserving apart from the shorter wait: same
  error handling, same retry/update scheduling, same provider contracts.
- Make the values and their application unit-testable.

**Non-Goals:**
- Introducing an explicit list of backend servers, health checks, or a
  server-discovery mechanism.
- Changing the periodic update interval, the wake-lock duration, or retry policy.
- Making the timeouts user-configurable preferences.
- Replacing `HttpURLConnection` with Retrofit/OkHttp.
- Changing authentication or data-source selection.

## Decisions

### Decision: Second-range values — 5 s connect, 10 s read

Use a connect timeout of `5_000` ms and a read timeout of `10_000` ms. This is the
maintainer-confirmed choice from review.

- **Why**: A TCP connect on mobile networks normally completes well under a
  second; 5 s tolerates slow networks while failing an unresponsive server quickly
  enough to take the next resolved DNS record or the next 60-second update. The read
  timeout is larger because a grid response can be sizeable on a slow link, but it
  still caps a server that accepts the connection and then hangs. Both are far below
  the previous 40 s and below any value that would outlive the 5-second background
  wake lock.
- **Alternative considered**: Keep the 40 s values. Rejected: that is the problem
  the issue reports. Use a single combined timeout for both phases. Rejected:
  connect and read bound different failure modes and deserve different values.
- **Alternative considered**: Use very aggressive values (1–2 s). Rejected as
  likely to abort legitimate slow mobile connections.
- **Note**: The spec bounds the values (connect ≤ 5 s, read ≤ 10 s) so they can be
  tuned during implementation without changing the requirement.

### Decision: One shared declaration instead of two literals

Declare the values once (proposed `org.blitzortung.android.util.NetworkTimeouts`
with `CONNECT_TIMEOUT_MS` and `READ_TIMEOUT_MS`, plus a small
`applyTo(URLConnection)` helper) and read them from both providers.

- **Why**: The two 40 s literals can drift apart; one definition makes the intent
  explicit and greppable. `util` is importable from both the `jsonrpc` and the
  `data.provider.blitzortung` packages without creating a dependency from the
  Blitzortung provider onto the JSON-RPC package.
- **Alternative considered**: Keep the constants inside `HttpServiceClient`.
  Rejected because `BlitzortungHttpDataProvider` does not use that interface and
  importing it just for constants is a cross-layer dependency.
- **Alternative considered**: Put the constants on the `DataProvider` interface.
  Rejected because the timeout is applied below the provider abstraction
  (`JsonRpcClient`), so the interface is not the natural owner.

### Decision: Keep the `HttpServiceClient` override mechanism, default it to the shared values

`HttpServiceClient` keeps its `connectionTimeout` / `socketTimeout` properties.
`HttpServiceClientDefault` initializes them from the shared constants instead of
`0`, and `JsonRpcClient` continues to set them from the shared constants in `init`
(replacing the `40000` literals). `BlitzortungHttpDataProvider` applies the shared
values through the helper.

- **Why**: The mutable properties are part of the existing (internal) interface
  and its tests; leaving them in place keeps the blast radius small while removing
  every raw literal from production code. Defaulting `HttpServiceClientDefault`
  from the shared values closes the "used directly, no override" edge.
- **Alternative considered**: Remove the mutable properties and always apply the
  constants. Rejected: larger, unnecessary API churn, and it would lose the
  ability for a focused test to configure a connection differently.

### Decision: Keep the timeouts as compile-time constants

Do not add a preference key or any runtime configuration for the timeout values;
keep them as compile-time constants. Confirmed by the maintainer in review.

- **Why**: A shorter timeout is a defensive default, not a user-facing tuning knob.
  No new preference keeps the settings surface, translations, and backup/restore
  behavior unchanged, and avoids exposing a value most users cannot reason about.
- **Alternative considered**: Expose the timeouts as a (debug) preference.
  Rejected: it adds settings and localization surface, plus backup/restore
  interaction, for a value that only helps unusual networks.

### Decision: Rely on DNS multi-record round-robin and the update cycle; do not add a server list

The host is published through DNS-based multi-record round-robin. When a connection
attempt to one resolved record fails, the client takes the next resolved record
within the same request, and it retries on the next scheduled update after a total
failure. The change reduces the timeout; it does not add a server list or health
checks. Confirmed by the maintainer in review.

- **Why**: The issue asks to reduce the timeout "for faster fallback to
  alternative servers". The alternative servers are already exposed through the
  host's DNS records; a shorter connect timeout is what makes taking the next record
  fast. Adding a managed server list, health checks, or a status endpoint is a
  distinct, larger feature with its own failure modes.
- **Alternative considered**: Hardcode a primary/secondary server list and retry
  across it. Rejected: it duplicates DNS, needs a health policy, and would silently
  ignore the user-configured `service_url` preference.

### Decision: Preserve existing failure semantics

A timeout continues to surface exactly as today: the JSON-RPC provider wraps the
`IOException` in a `DataProviderException`, which `FetchDataTask` converts into a
failed `DataReceived`; the Blitzortung provider catches
`SocketException` / `SocketTimeoutException` while reading and returns an empty
result. No new crash paths, no user-facing error beyond the existing failed state.

- **Why**: The issue is about latency, not error reporting. Reusing the existing
  degradation keeps the change reviewable and reversible.

## Risks / Trade-offs

- **Too-short read timeout on slow links** → Aborts a large grid response and
  reports a failed update. Mitigation: read timeout stays at 10 s, larger than the
  5 s connect timeout; the next cycle retries.
- **Fallback depends on DNS record selection** → On a hang after connect (rather
  than a refused connect) the client may not take another record within the same
  request. Mitigation: the read timeout bounds the hang and the periodic update
  performs the retry; known limitation.
- **Background wake lock interaction** → A request that now fails in seconds may
  finish after the wake lock is released on slow devices. This only changes timing,
  not correctness; `FetchBackgroundDataTask` already releases the lock on
  completion.
- **Hidden callers of the timeout literals** → If another site relies on 40 s.
  Mitigation: the change greps for all `40000` timeout sites; only the two known
  providers exist today.
- **Value drift between spec and code** → The spec bounds the values (≤ 5 s / ≤
  10 s) rather than pinning exact numbers, and tests assert the bound, so tuning
  does not invalidate the spec.

## Migration Plan

1. Add the shared `NetworkTimeouts` declaration and its unit test.
2. Replace the `40000` literals in `JsonRpcClient` and default
   `HttpServiceClientDefault` from the shared values; extend `JsonRpcClientTest`
   and `HttpServiceClientDefaultTest`.
3. Replace the `40000` literals in `BlitzortungHttpDataProvider.readFromUrl()`
   with the shared values (via the helper) and cover the helper with a test.
4. Run `./gradlew testDebugUnitTest` and `./gradlew lint`.

**Rollback**: revert the single change; the two call sites return to the shared
constants, and restoring 40 s (or the previous literals) fully reverts the
behavior. There is no stored state, migration, or configuration to undo.
