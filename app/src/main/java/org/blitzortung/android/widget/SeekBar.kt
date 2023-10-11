package org.blitzortung.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatSeekBar
import org.blitzortung.android.app.Main

class SeekBar : AppCompatSeekBar {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val solidColor = Color.CYAN
        val paint = Paint()
        paint.color = solidColor
        val cx = height / 2f
        val cy = 10f
        c.drawCircle(cx, cy, 5f, paint)
        Log.v(Main.LOG_TAG, "SeekBar.onDraw() ${cx}, ${cy}")
    }
}