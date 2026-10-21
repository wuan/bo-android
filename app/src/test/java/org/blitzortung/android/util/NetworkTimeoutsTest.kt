package org.blitzortung.android.util

import java.net.URL
import java.net.URLConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NetworkTimeoutsTest {
    @Test
    fun connectTimeoutIsPositiveAndBounded() {
        assertThat(NetworkTimeouts.CONNECT_TIMEOUT_MS).isPositive()
        assertThat(NetworkTimeouts.CONNECT_TIMEOUT_MS).isLessThanOrEqualTo(5_000)
    }

    @Test
    fun readTimeoutIsPositiveAndBounded() {
        assertThat(NetworkTimeouts.READ_TIMEOUT_MS).isPositive()
        assertThat(NetworkTimeouts.READ_TIMEOUT_MS).isLessThanOrEqualTo(10_000)
    }

    @Test
    fun appliesBothTimeoutsToConnection() {
        val connection = TestUrlConnection()

        NetworkTimeouts.applyTo(connection)

        assertThat(connection.connectTimeout).isEqualTo(NetworkTimeouts.CONNECT_TIMEOUT_MS)
        assertThat(connection.readTimeout).isEqualTo(NetworkTimeouts.READ_TIMEOUT_MS)
    }

    private class TestUrlConnection : URLConnection(URL("http://base.url/")) {
        override fun connect(): Unit = Unit
    }
}
