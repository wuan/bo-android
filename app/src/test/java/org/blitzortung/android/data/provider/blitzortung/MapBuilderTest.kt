package org.blitzortung.android.data.provider.blitzortung

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.beans.DefaultStrike
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class MapBuilderTest {

    private lateinit var mapBuilderFactory: MapBuilderFactory

    @Before
    fun setUp() {
        mapBuilderFactory = MapBuilderFactory()
    }


    @Test
    fun parsesLine() {
        val line =
            "2019-02-13 06:07:24.748241830 pos;37.714012;26.801138;0 str;0 dev;7013 sta;18;85;1903,2326,2325,1870,1326,1425,1871,2356,1300,1575,1868,1825,803,1463,2134,2159,1866,2365,1319,867,1913,1757,2191,2192,1777,1309,2196,1308,2187,806,2209,1775,1761,1981,2193,2198,1762,1931,2213,1926,2217,2197,1972,1667,1947,730,1644,1269,1716,1443,708,1796,1654,1730,1519,1681,1768,894,2204,1150,1801,1359,1259,2199,2070,1752,2117,869,1088,2215,1766,1788,1712,1989,1071,675,1073,699,1994,994,1733,1609,1422,1862,2232"

        val uut = mapBuilderFactory.createStrikeMapBuilder()

        val strike = uut.buildFromLine(line)

        assertThat(strike).isInstanceOf(DefaultStrike::class.java)
        val defaultStrike = strike as DefaultStrike
        assertThat(Instant.ofEpochMilli(strike.timestamp)).isEqualTo(Instant.parse("2019-02-13T06:07:24.748Z"))
        assertThat(defaultStrike.latitude).isEqualTo(37.714012)
        assertThat(defaultStrike.longitude).isEqualTo(26.801138)
        assertThat(defaultStrike.altitude).isEqualTo(0)
        assertThat(defaultStrike.amplitude).isEqualTo(0.0f)
        assertThat(strike.multiplicity).isEqualTo(1)
    }

    @Test
    fun handlesEmptyLine() {
        val line = ""
        val uut = mapBuilderFactory.createStrikeMapBuilder()

        val strike = uut.buildFromLine(line)

        assertThat(strike).isNull()
    }

    @Test
    fun handlesEmptyPosition() {
        val line = "2019-02-13 06:07:24.7482418"
        val uut = mapBuilderFactory.createStrikeMapBuilder()

        val strike = uut.buildFromLine(line)

        assertThat(strike).isNull()
    }

    @Test
    fun handlesIncompletePosition() {
        val line = "2019-02-13 06:07:24.7482418 pos;23;32"
        val uut = mapBuilderFactory.createStrikeMapBuilder()

        val strike = uut.buildFromLine(line)

        assertThat(strike).isNull()
    }

    @Test
    fun handlesMinimalStrikeData() {
        val line = "2019-02-13 06:07:24.7482418 pos;23;32;0"
        val uut = mapBuilderFactory.createStrikeMapBuilder()

        val strike = uut.buildFromLine(line)

        assertThat(strike).isNotNull
    }
}