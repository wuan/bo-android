package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShapeLabelTest {
    private val paint = Paint()

    @Test
    fun drawsCenteredLabelForLargeEnoughShapes() {
        val canvas = mockk<Canvas>(relaxed = true)
        val bounds = RectF(0f, 0f, 100f, 100f)

        drawShapeLabel(canvas, paint, bounds, "42", 0xFF000000.toInt())

        verify(exactly = 1) { canvas.drawText("42", any(), any(), any()) }
    }

    @Test
    fun skipsLabelWhenShapeIsTooSmall() {
        val canvas = mockk<Canvas>(relaxed = true)
        // height / SHAPE_LABEL_TEXT_SIZE_DIVISOR stays below MIN_SHAPE_LABEL_TEXT_SIZE
        val bounds = RectF(0f, 0f, 10f, MIN_SHAPE_LABEL_TEXT_SIZE * SHAPE_LABEL_TEXT_SIZE_DIVISOR - 1f)

        drawShapeLabel(canvas, paint, bounds, "42", 0xFF000000.toInt())

        verify(exactly = 0) { canvas.drawText(any(), any(), any(), any()) }
    }
}
