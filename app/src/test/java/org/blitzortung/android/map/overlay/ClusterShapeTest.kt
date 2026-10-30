package org.blitzortung.android.map.overlay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ClusterShapeTest {
    @Test
    fun wrapPixelXShiftsVertexAcrossTheDateLineIntoTheSameWorldCopy() {
        // world width 720px; a vertex projected to the far left (x = 4) while the
        // previous vertex sits near the right edge (x = 716) is a date line
        // crossing: it must be shifted one world to the right instead of drawing a
        // line across the whole map.
        assertThat(wrapPixelX(x = 4.0, referenceX = 716.0, worldSize = 720.0)).isEqualTo(724.0)
    }

    @Test
    fun wrapPixelXLeavesNearbyVertexUnchanged() {
        assertThat(wrapPixelX(x = 718.0, referenceX = 716.0, worldSize = 720.0)).isEqualTo(718.0)
    }

    @Test
    fun wrapPixelXShiftsVertexLeftWhenItIsMoreThanHalfAWorldAhead() {
        assertThat(wrapPixelX(x = 1420.0, referenceX = 716.0, worldSize = 720.0)).isEqualTo(700.0)
    }

    @Test
    fun wrapPixelXReturnsInputWhenWorldSizeIsUnknown() {
        assertThat(wrapPixelX(x = 4.0, referenceX = 716.0, worldSize = 0.0)).isEqualTo(4.0)
    }
}
