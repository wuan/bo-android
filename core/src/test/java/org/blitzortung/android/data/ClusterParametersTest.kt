package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ClusterParametersTest {
    @Test
    fun defaultsToASingleTenMinuteInterval() {
        val parameters = ClusterParameters()

        assertThat(parameters.minuteLength).isEqualTo(10)
        assertThat(parameters.intervalCount).isEqualTo(1)
        assertThat(parameters.minuteOffset).isEqualTo(0)
        assertThat(parameters.global).isTrue()
    }

    @Test
    fun derivesIntervalCountFromTimeInterval() {
        assertThat(ClusterParameters.intervalCountFor(30)).isEqualTo(3)
        assertThat(ClusterParameters.intervalCountFor(60)).isEqualTo(6)
        assertThat(ClusterParameters.intervalCountFor(90)).isEqualTo(9)
        assertThat(ClusterParameters.intervalCountFor(120)).isEqualTo(12)
        assertThat(ClusterParameters.intervalCountFor(240)).isEqualTo(24)
        assertThat(ClusterParameters.intervalCountFor(0)).isEqualTo(1)
    }

    @Test
    fun carriesExplicitIntervalCount() {
        val parameters = ClusterParameters(intervalCount = 12)

        assertThat(parameters.intervalCount).isEqualTo(12)
        assertThat(parameters.minuteLength).isEqualTo(10)

        assertThat(ClusterParameters().withIntervalCount(3).intervalCount).isEqualTo(3)
    }

    @Test
    fun appliesNegativeHistoryOffset() {
        val parameters = ClusterParameters(intervalCount = 6).withMinuteOffset(-30)

        assertThat(parameters.minuteOffset).isEqualTo(-30)
        assertThat(parameters.minuteLength).isEqualTo(10)
        assertThat(parameters.intervalCount).isEqualTo(6)
    }

    @Test
    fun selectsLocalWhenDataAreaIsSet() {
        val dataArea = DataArea(4, 9, 5)

        val parameters = ClusterParameters().withDataArea(dataArea)

        assertThat(parameters.global).isFalse()
        assertThat(parameters.dataArea).isEqualTo(dataArea)
    }
}
