package org.blitzortung.android.util

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.assertThat
import org.junit.Test

class TimeFormatTest {

    @Test
    fun testParseTimeWithMilliseconds() {
        val result = TimeFormat.parseTimeWithMilliseconds("20120901T20:10:05.123")

        assertThat(result).isEqualTo(1346530205123L)
    }

    @Test
    fun testParseTimeWithMillisecondsWithoutMillisecondsInString() {
        assertThatThrownBy { TimeFormat.parseTimeWithMilliseconds("20120901T20:10:05") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Unable to parse millisecond time string '20120901T20:10:05'")
    }

    @Test
    fun testParseTime() {
        val result = TimeFormat.parseTime("20120901T20:10:05")

        assertThat(result).isEqualTo(1346530205000L)
    }

    @Test
    fun testParseTimeWithAdditinalMillisecondsInString() {

        val result = TimeFormat.parseTime("20120901T20:10:05.123")

        assertThat(result).isEqualTo(1346530205000L)
    }

    @Test
    fun testParseTimeWithBadString() {
        assertThatThrownBy { TimeFormat.parseTime("20120901T20:10") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Unable to parse time string '20120901T20:10'")
    }
}
