package org.blitzortung.android.app.view

import android.content.Context
import android.util.AttributeSet
import org.blitzortung.android.app.R
import org.blitzortung.android.util.TabletAwareView
import androidx.core.content.withStyledAttributes

class ScaledTextView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    init {
        context.withStyledAttributes(attrs, R.styleable.View, 0, 0) {

            val scaleForTablet =
                getBoolean(R.styleable.View_tablet_scaleable, false) && TabletAwareView.isTablet(context)

            if (scaleForTablet) {
                val displayMetrics = context.resources.displayMetrics
                textSize *= TabletAwareView.textSizeFactor(scaleForTablet) / displayMetrics.scaledDensity
            }
        }
    }
}
