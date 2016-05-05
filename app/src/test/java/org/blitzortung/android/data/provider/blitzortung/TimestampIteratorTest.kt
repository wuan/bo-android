package org.blitzortung.android.data.provider.blitzortung

import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Test

class TimestampIteratorTest {

    @Test
    fun checkNormalOperation() {

        assertThat(TimestampIterator(1000, 10001, 12999))
                .containsExactly(10000L, 11000L, 12000L)

        assertThat(TimestampIterator(999, 9990, 12987))
                .containsExactly(9990L, 10989L, 11988L, 12987L)
    }

    @Test
    fun createsTimestampSequence() {
        val endTime:Long = 12300
        val startTime: Long = endTime - 4000
        val intervalLength: Long = 1000

        val sequence = createTimestampSequence(intervalLength, startTime, endTime)

        assertThat(sequence).containsExactly(8000L, 9000L, 10000L, 11000L, 12000L)
    }
}