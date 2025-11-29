package org.blitzortung.android.app.view.support

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap

class CanvasProvider(
    val width: Int,
    val height: Int
) {
    private var canvas: CanvasWrapper? = null

    fun provide(backgroundColor: Int, width: Int, height: Int): CanvasWrapper? {
        if (canvas == null) {
            canvas = CanvasWrapper(width, height, backgroundColor)
        }
        return canvas
    }
}

class CanvasWrapper(
    width: Int,
    height: Int,
    backgroundColor: Int,
) {

    private val bitmap: Bitmap = createBitmap(width, height)
    val canvas: Canvas = Canvas(bitmap)
    val background: Paint = Paint()
    private val transfer = Paint()

    init {
        background.color = backgroundColor
    }

    fun clear() {
        background.xfermode = XFERMODE_CLEAR
        canvas.drawPaint(background)
        background.xfermode = XFERMODE_SRC
    }

    fun update(targetCanvas: Canvas) {
        targetCanvas.drawBitmap(bitmap, 0f, 0f, transfer)
    }

    companion object {
        private val XFERMODE_CLEAR = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        private val XFERMODE_SRC = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

}
