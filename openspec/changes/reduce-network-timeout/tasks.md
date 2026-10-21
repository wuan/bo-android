## 1. Shared timeout definition

- [ ] 1.1 Add a shared declaration (proposed `org.blitzortung.android.util.NetworkTimeouts`) with `CONNECT_TIMEOUT_MS = 5_000`, `READ_TIMEOUT_MS = 10_000`, and a helper that applies them to a `java.net.URLConnection`, and verify `./gradlew assembleDebug` compiles
- [ ] 1.2 Add `NetworkTimeoutsTest` asserting both values are positive, that the connect timeout is at most 5 000 ms and the read timeout is at most 10 000 ms, and that the helper sets `connectTimeout`/`readTimeout` on a connection

## 2. JSON-RPC path

- [ ] 2.1 Replace the `40000` literals in `app/src/main/java/org/blitzortung/android/jsonrpc/JsonRpcClient.kt` with the shared constants for `connectionTimeout` and `socketTimeout`, and verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.jsonrpc.JsonRpcClientTest"` passes
- [ ] 2.2 Initialize `connectionTimeout`/`socketTimeout` in `app/src/main/java/org/blitzortung/android/jsonrpc/HttpServiceClientDefault.kt` from the shared constants instead of `0`, so a directly constructed client is also bounded
- [ ] 2.3 Extend `JsonRpcClientTest` to assert the client configures both timeouts from the shared constants, and extend `HttpServiceClientDefaultTest` (its mock connection) to assert the applied connection timeouts, and verify `./gradlew testDebugUnitTest --tests "org.blitzortung.android.jsonrpc.*"` passes

## 3. Blitzortung HTTP path

- [ ] 3.1 In `app/src/main/java/org/blitzortung/android/data/provider/blitzortung/BlitzortungHttpDataProvider.kt`'s `readFromUrl()`, replace the `40000` literals with the shared values and confirm its `SocketException`/`SocketTimeoutException` handling still returns an empty result
- [ ] 3.2 Add a test that exercises the shared helper against a fake connection (a `URLStreamHandler` passed directly to the `URL` constructor, or a mock `URLConnection`) so the Blitzortung path's timeout configuration is covered without relying on a global `URLStreamHandlerFactory`

## 4. Quality gate

- [ ] 4.1 Run `./gradlew testDebugUnitTest` and verify all new and existing unit tests pass
- [ ] 4.2 Run `./gradlew lint` and verify no new lint errors are introduced
- [ ] 4.3 Verify behavior manually (or with an instrumented/integration check): point `service_url` at a non-routable or black-holed host and confirm the request fails within seconds, reports a failed result, and the app keeps cycling updates without crashing
