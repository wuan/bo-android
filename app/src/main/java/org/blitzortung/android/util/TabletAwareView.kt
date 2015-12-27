package org.blitzortung.android.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper

open class TabletAwareView(context: Context, attrs: AttributeSet?, defStyle: Int) : View(context, attrs, defStyle) {

    protected val padding: Float
    protected val textSize: Float
    protected val sizeFactor: Float

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.View, defStyle, 0);

        val scaleForTablet = a.getBoolean(R.styleable.View_tablet_scaleable, false) && isTablet(context)

        padding = ViewHelper.pxFromDp(this, padding(scaleForTablet))
        textSize = ViewHelper.pxFromDp(this, textSize(scaleForTablet))
        sizeFactor = sizeFactor(scaleForTablet)

        a.recycle();
    }

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    @SuppressWarnings("unused")
    constructor(context: Context) : this(context, null, 0) {
    }

    companion object {
        fun isTablet(context: Context): Boolean {
            return context.resources.configuration.smallestScreenWidthDp >= 600;
        }

        fun padding(context: Context) :Float {
            return padding(isTablet(context))
        }

        fun padding(scaleForTablet: Boolean) :Float {
            return if (scaleForTablet) 8f else 5f
        }

        fun textSize(context: Context) :Float {
            return textSize(isTablet(context))
        }

        fun textSize(scaleForTablet: Boolean) :Float {
            return if (scaleForTablet) 18f else 12f
        }

        fun sizeFactor(scaleForTablet: Boolean) : Float {
            return if (scaleForTablet) 1.8f else 1f
        }
    }

}

