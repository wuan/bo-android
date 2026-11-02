/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.util

import java.net.URLConnection

/**
 * Bounded connect and read timeouts for outbound lightning-data HTTP requests.
 *
 * The values live in the seconds range so an unresponsive backend server fails
 * fast: the client can then take the next resolved DNS record within the same
 * request or retry on the next scheduled update, instead of blocking for tens of
 * seconds and letting a fetch outlive the 5-second background wake lock.
 *
 * These are compile-time constants and are intentionally not user-configurable.
 */
object NetworkTimeouts {
    const val CONNECT_TIMEOUT_MS = 5_000
    const val READ_TIMEOUT_MS = 10_000

    fun applyTo(connection: URLConnection) {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
    }
}
