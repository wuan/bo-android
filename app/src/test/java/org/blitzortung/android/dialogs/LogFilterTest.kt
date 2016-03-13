package org.blitzortung.android.dialogs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LogFilterTest {

    val logFilter = LogFilter()

    @Test
    fun shouldMatchLine() {
        assertThat(logFilter.forLine("V/BO_ANDROID( 1234): log message")).isEqualTo("log message")
    }
}