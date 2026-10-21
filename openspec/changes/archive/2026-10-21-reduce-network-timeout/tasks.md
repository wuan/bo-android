## 1. Shared timeout definition

- [x] 1.1 Add a shared declaration (proposed `org.blitzortung.android.util.NetworkTimeouts`) with `CONNECT_TIMEOUT_MS = 5_000`, `READ_TIMEOUT_MS = 10_000`, and a helper that applies them to a `java.net.URLConnection`, and verify `./gradlew :app:compileDebugKotlin` compiles
- [x] 1.2 Add `NetworkTimeoutsTest` asserting both values are positive, that the connect timeout is at most 5 000 ms and the read timeout is at most 10 000 ms, and that the helper sets `connectTimeout`/`readTimeout` on a connection

## 2. JSON-RPC path

- [x] 2.1 Replace the `40000` literals in `app/src/main/java/org/blitzortung/android/jsonrpc/JsonRpcClient.kt` with the shared constants for `connectionTimeout` and `socketTimeout`, and verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.jsonrpc.JsonRpcClientTest"` passes
- [x] 2.2 Initialize `connectionTimeout`/`socketTimeout` in `app/src/main/java/org/blitzortung/android/jsonrpc/HttpServiceClientDefault.kt` from the shared constants instead of `0`, so a directly constructed client is also bounded
- [x] 2.3 Extend `JsonRpcClientTest` to assert the client configures both timeouts from the shared constants, and extend `HttpServiceClientDefaultTest` (its mock connection) to assert the applied connection timeouts, and verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.jsonrpc.*"` passes

## 3. Blitzortung HTTP path

- [x] 3.1 In `app/src/main/java/org/blitzortung/android/data/provider/blitzortung/BlitzortungHttpDataProvider.kt`'s `readFromUrl()`, replace the `40000` literals by applying the shared helper (`NetworkTimeouts.applyTo(connection)`), keep setting `allowUserInteraction = false`, and confirm its `SocketException`/`SocketTimeoutException` handling still returns an empty result
- [x] 3.2 Confirm the shared-helper test added in task 1.2 is the single coverage for the timeout values used by this path (no separate provider-level test is required, since `readFromUrl()` has no injectable connection seam and building one is out of scope)

## 4. Quality gate

- [x] 4.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [x] 4.2 Run `./gradlew lint` and verify no new lint errors are introduced
- [ ] 4.3 Manual verification (not unit-testable; do not block the change on it): point `service_url` at a non-routable or black-holed host and confirm the request fails within seconds, reports a failed result, and the app keeps cycling updates without crashing
- [x] 4.4 Confirm the timeouts remain compile-time constants — no new preference key or runtime configuration is introduced, and no settings/translation/backup surface changes (reviewer decision)
- [x] 4.5 Confirm no new code implements DNS record selection: the fallback requirement is satisfied by the platform's multi-record resolution plus the bounded connect timeout, so no server list, health check, or extra request logic is added
