package org.blitzortung.android.data

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.beans.GridParameters
import org.junit.Test

class GridParametersTest {
    private val parameters = GridParameters(10.0, 45.0, 0.2, 0.2, 20, 15)

    @Test
    fun testRectCenter() {
        assertThat(parameters.rectCenterLongitude).isEqualTo(12.0)
        assertThat(parameters.rectCenterLatitude).isEqualTo(43.5)
    }

    @Test
    fun testElementCenter() {
        assertThat(parameters.getCenterLongitude(0)).isEqualTo(10.1)
        assertThat(parameters.getCenterLatitude(0)).isEqualTo(44.9)

        assertThat(parameters.getCenterLongitude(19)).isEqualTo(13.9)
        assertThat(parameters.getCenterLatitude(19)).isEqualTo(41.1)
    }

//    @Test
//    fun testGetRect() {
//        val screenRect = Rect(10, 20, 30, 40)
//        val center = GeoPoint(42.5, 12.5)
//        val projection = Projection(5.0, screenRect, center, 0.0, 0, 0.0, false, false, null, 0, 0)
//        parameters.getRect(projection)
//    }

    @Test
    fun containsWithoutInset() {
        assertThat(parameters.contains(10.0, 42.0)).isTrue
        assertThat(parameters.contains(14.0, 42.0)).isTrue
        assertThat(parameters.contains(14.0, 45.0)).isTrue
        assertThat(parameters.contains(10.0, 45.0)).isTrue
        assertThat(parameters.contains(12.0, 43.5)).isTrue

        assertThat(parameters.contains(9.9, 42.0)).isFalse
        assertThat(parameters.contains(10.0, 41.9)).isFalse
        assertThat(parameters.contains(14.1, 45.0)).isFalse
        assertThat(parameters.contains(14.0, 45.1)).isFalse
    }

    @Test
    fun containsWithPositiveInset() {
        assertThat(parameters.contains(10.5, 42.5, 0.5)).isTrue
        assertThat(parameters.contains(13.5, 42.5, 0.5)).isTrue
        assertThat(parameters.contains(13.5, 44.5, 0.5)).isTrue
        assertThat(parameters.contains(10.5, 44.5, 0.5)).isTrue

        assertThat(parameters.contains(10.4, 42.5, 0.5)).isFalse
        assertThat(parameters.contains(10.5, 42.4, 0.5)).isFalse
        assertThat(parameters.contains(13.6, 44.5, 0.5)).isFalse
        assertThat(parameters.contains(13.5, 44.6, 0.5)).isFalse
    }
}
