package org.blitzortung.android.util

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat

class TimeFormatTest {

    @Rule
    var expectedException = ExpectedException.none()

    @Test
    fun testParseTimeWithMilliseconds() {

        val result = TimeFormat.parseTimeWithMilliseconds("20120901T20:10:05.123")

        assertThat(result, `is`(1346530205123L))
    }

    @Test
    fun testParseTimeWithMillisecondsWithoutMillisecondsInString() {

        expectedException.expect(IllegalArgumentException::class.java)
        expectedException.expectMessage("Unable to parse millisecond time string '20120901T20:10:05'")

        val result = TimeFormat.parseTimeWithMilliseconds("20120901T20:10:05")

        assertThat(result, `is`(0L))
    }

    @Test
    fun testParseTime() {

        val result = TimeFormat.parseTime("20120901T20:10:05")

        assertThat(result, `is`(1346530205000L))
    }

    @Test
    fun testParseTimeWithAdditinalMillisecondsInString() {

        val result = TimeFormat.parseTime("20120901T20:10:05.123")

        assertThat(result, `is`(1346530205000L))
    }

    @Test
    fun testParseTimeWithBadString() {

        expectedException.expect(IllegalArgumentException::class.java)
        expectedException.expectMessage("Unable to parse time string '20120901T20:10'")

        val result = TimeFormat.parseTime("20120901T20:10")

        assertThat(result, `is`(0L))
    }
}
