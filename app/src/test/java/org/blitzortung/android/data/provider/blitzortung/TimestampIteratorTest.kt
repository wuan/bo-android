package org.blitzortung.android.data.provider.blitzortung

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class TimestampIteratorTest {

    @Test
    fun checkNormalOperation() {

        assertThat(TimestampIterator(1000, 10001, 12999))
            .toIterable()
            .containsExactly(10000L, 11000L, 12000L)

        assertThat(TimestampIterator(999, 9990, 12987))
            .toIterable()
            .containsExactly(9990L, 10989L, 11988L, 12987L)
    }

    @Test
    fun createsTimestampSequence() {
        val endTime: Long = 12300
        val startTime: Long = endTime - 4000
        val intervalLength: Long = 1000

        val sequence = createTimestampSequence(intervalLength, startTime, endTime)

        assertThat(sequence.asIterable()).containsExactly(8000L, 9000L, 10000L, 11000L, 12000L)
    }

    @Test
    fun throwsWhenExhausted() {
        val uut = TimestampIterator(10, 1000, 1000)

        assertThat(uut.next()).isEqualTo(1000)
        assertThatThrownBy { uut.next() }.isInstanceOf(NoSuchElementException::class.java)
    }
}
