package org.blitzortung.android.settings

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.blitzortung.android.app.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AppTheme is a fixed dark theme, so keep light system-bar icons regardless of the
        // system light/dark setting.
        // Known accepted Play warning: this AndroidX helper writes
        // LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES on API 28-29. See AGENTS.md
        // ("Edge-to-Edge Display") and
        // openspec/changes/archive/2026-11-02-accept-edge-to-edge-cutout-warning/.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContentView(R.layout.activity_settings)
    }
}
