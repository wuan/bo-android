package org.blitzortung.android.app.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import org.blitzortung.android.app.R
import org.blitzortung.android.util.TabletAwareView

// The tablet_scaleable styleable is intentionally shared with TabletAwareView, so it is not named
// after a single view class.
@SuppressLint("CustomViewStyleable")
class ScaledTextView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    init {
        context.withStyledAttributes(attrs, R.styleable.View, 0, 0) {

            val scaleForTablet =
                getBoolean(R.styleable.View_tablet_scaleable, false) && TabletAwareView.isTablet(context)

            if (scaleForTablet) {
                val displayMetrics = context.resources.displayMetrics
                @Suppress("DEPRECATION")
                textSize *= TabletAwareView.textSizeFactor(scaleForTablet) / displayMetrics.scaledDensity
            }
        }
    }
}
