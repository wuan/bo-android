package org.blitzortung.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PeriodTest {

    private val currentTime = Period.currentTime

    private val currentPeriod = 60

    private lateinit var period: Period

    @Before
    fun setUp() {
        period = Period()
    }

    @Test
    fun shouldUpdateReturnsTrueWhenCalledForTheFirstTime() {
        assertThat(period.shouldUpdate(Period.currentTime, 1000)).isTrue()
    }

    @Test
    fun shouldNotUpdateWhenCurrentPeriodIsNotOver() {
        period.shouldUpdate(currentTime, currentPeriod)

        assertThat(period.shouldUpdate(currentTime + currentPeriod - 1, currentPeriod)).isFalse()
    }

    @Test
    fun shouldUpdateWhenCurrentPeriodIsOver() {
        period.shouldUpdate(currentTime, currentPeriod)

        assertThat(period.shouldUpdate(currentTime + currentPeriod, currentPeriod)).isTrue()
    }

    @Test
    fun getCurrentUpdatePeriodShouldReturnTimeUntilNextUpdate() {
        period.shouldUpdate(currentTime, currentPeriod)

        assertThat(period.getCurrentUpdatePeriod(currentTime + 20, currentPeriod)).isEqualTo(40)
    }

    @Test
    fun isNthUpdateShouldCountUpdates() {
        period.shouldUpdate(currentTime, currentPeriod)

        assertThat(period.updateCount).isEqualTo(1)
        assertThat(period.isNthUpdate(1)).isTrue()
        assertThat(period.isNthUpdate(2)).isFalse()
        assertThat(period.isNthUpdate(3)).isFalse()

        period.shouldUpdate(currentTime + currentPeriod, currentPeriod)

        assertThat(period.updateCount).isEqualTo(2)
        assertThat(period.isNthUpdate(1)).isTrue()
        assertThat(period.isNthUpdate(2)).isTrue()
        assertThat(period.isNthUpdate(3)).isFalse()

        period.shouldUpdate(currentTime + 2 * currentPeriod, currentPeriod)

        assertThat(period.updateCount).isEqualTo(3)
        assertThat(period.isNthUpdate(1)).isTrue()
        assertThat(period.isNthUpdate(2)).isFalse()
        assertThat(period.isNthUpdate(3)).isTrue()
    }

    @Test
    fun restartShouldResetCountAndLastUpdateTime() {
        period.shouldUpdate(currentTime, currentPeriod)

        period.restart()

        assertThat(period.lastUpdateTime).isEqualTo(0)
        assertThat(period.updateCount).isEqualTo(0)
    }
}
