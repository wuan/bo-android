package org.blitzortung.android.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceFragmentCompat
import org.blitzortung.android.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    public override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)

//        setContentView(R.layout.activity_settings)

//        val yourToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.your_toolbar)
//
//        ViewCompat.setOnApplyWindowInsetsListener(yourToolbar) { view, windowInsets ->
//            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
//
//            // Apply the insets as padding to the view.
//            // This will push the toolbar's content down, but not the toolbar itself.
//            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
//
//            // Return the insets so other views can also use them
//            WindowInsetsCompat.CONSUMED
//        }
    }

    //    override fun onViewCreated(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // This line is crucial to enable edge-to-edge display
//    }
}