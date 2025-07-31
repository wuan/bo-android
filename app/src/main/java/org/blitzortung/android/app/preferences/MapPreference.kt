package org.blitzortung.android.app.preferences

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceManager
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.getInt
import org.blitzortung.android.app.getString
import org.blitzortung.android.app.putInt
import org.blitzortung.android.app.putString
import org.blitzortung.android.app.view.PreferenceKey

class MapPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
}

// Assuming your original PreferenceScreen key was "map_settings_screen" for example
class MapPreferenceDialogFragment : PreferenceDialogFragmentCompat() {

    private lateinit var radioGroupMapMode: RadioGroup
    private lateinit var seekBarMapScale: SeekBar
    private lateinit var textViewMapScaleValue: TextView
    private lateinit var seekBarMapFade: SeekBar
    private lateinit var textViewMapFadeValue: TextView
    private lateinit var radioGroupColorScheme: RadioGroup

    // Define keys from your original preferences.xml for SharedPreferences
    companion object {
        // Min/Max/Increment for SeekBars based on your XML
        private const val MAP_SCALE_MIN = 25
        private const val MAP_SCALE_MAX = 150
        private const val MAP_SCALE_INCREMENT = 25

        private const val MAP_FADE_MIN = 0 // Assuming 0 if not specified, adjust if needed
        private const val MAP_FADE_MAX = 90
        private const val MAP_FADE_INCREMENT = 5


        fun newInstance(): MapPreferenceDialogFragment {
            return MapPreferenceDialogFragment()
        }
    }

    override fun onCreateDialogView(context: Context): View {
        Log.v(LOG_TAG, "MapPreferenceDialogFragment.onCreateDialogView()")
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.settings_map, null) // Your new layout

        radioGroupMapMode = view.findViewById(R.id.radioGroupMapMode)
        seekBarMapScale = view.findViewById(R.id.seekBarMapScale)
        textViewMapScaleValue = view.findViewById(R.id.textViewMapScaleValue)
        seekBarMapFade = view.findViewById(R.id.seekBarMapFade)
        textViewMapFadeValue = view.findViewById(R.id.textViewMapFadeValue)
        radioGroupColorScheme = view.findViewById(R.id.radioGroupColorScheme)

        loadCurrentPreferences()
        setupListeners()

        return view
    }

    private fun loadCurrentPreferences() {
        Log.v(LOG_TAG, "MapPreferenceDialogFragment.loadCurrentPreferences()")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // --- Map Mode ---
        val mapModeEntries = resources.getStringArray(R.array.map_modes) // From your arrays.xml
        val mapModeValues = resources.getStringArray(R.array.map_mode_values)
        val currentMapModeValue = sharedPreferences.getString(PreferenceKey.MAP_TYPE, "SATELLITE")

        radioGroupMapMode.removeAllViews() // Clear any existing (e.g., from XML preview)
        for (i in mapModeEntries.indices) {
            val radioButton = RadioButton(requireContext()).apply {
                text = mapModeEntries[i]
                tag = mapModeValues[i] // Store the value in the tag
                id = View.generateViewId()
            }
            radioGroupMapMode.addView(radioButton)
            if (mapModeValues[i] == currentMapModeValue) {
                radioButton.isChecked = true
            }
        }

        // --- Map Scale ---
        val currentMapScale = sharedPreferences.getInt(PreferenceKey.MAP_SCALE, 70)
        // Adjust SeekBar max and progress for min value
        seekBarMapScale.max = (MAP_SCALE_MAX - MAP_SCALE_MIN) / MAP_SCALE_INCREMENT
        seekBarMapScale.progress = (currentMapScale - MAP_SCALE_MIN) / MAP_SCALE_INCREMENT
        updateMapScaleText(currentMapScale)


        // --- Map Fade ---
        val currentMapFade = sharedPreferences.getInt(PreferenceKey.MAP_FADE, 55)
        seekBarMapFade.max = (MAP_FADE_MAX - MAP_FADE_MIN) / MAP_FADE_INCREMENT
        seekBarMapFade.progress = (currentMapFade - MAP_FADE_MIN) / MAP_FADE_INCREMENT
        updateMapFadeText(currentMapFade)

        // --- Color Scheme ---
        val colorSchemeEntries = resources.getStringArray(R.array.color_schemes)
        val colorSchemeValues = resources.getStringArray(R.array.color_scheme_values)
        val currentColorValue = sharedPreferences.getString(PreferenceKey.COLOR_SCHEME, "BLITZORTUNG")

        radioGroupColorScheme.removeAllViews()
        for (i in colorSchemeEntries.indices) {
            val radioButton = RadioButton(requireContext()).apply {
                text = colorSchemeEntries[i]
                tag = colorSchemeValues[i]
                id = View.generateViewId()
            }
            radioGroupColorScheme.addView(radioButton)
            if (colorSchemeValues[i] == currentColorValue) {
                radioButton.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        seekBarMapScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualValue = MAP_SCALE_MIN + (progress * MAP_SCALE_INCREMENT)
                updateMapScaleText(actualValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarMapFade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualValue = MAP_FADE_MIN + (progress * MAP_FADE_INCREMENT)
                updateMapFadeText(actualValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateMapScaleText(value: Int) {
        textViewMapScaleValue.text = getString(R.string.percent_format, value) // Assuming "%d%%" or similar
    }

    private fun updateMapFadeText(value: Int) {
        textViewMapFadeValue.text = getString(R.string.percent_format, value)
    }


    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            sharedPreferences.edit {

                // Save Map Mode
                val selectedMapModeId = radioGroupMapMode.checkedRadioButtonId
                if (selectedMapModeId != -1) {
                    val selectedRadioButton = radioGroupMapMode.findViewById<RadioButton>(selectedMapModeId)
                    putString(PreferenceKey.MAP_TYPE, selectedRadioButton.tag as String)
                }

                // Save Map Scale
                val mapScaleProgress = seekBarMapScale.progress
                val mapScaleValue = MAP_SCALE_MIN + (mapScaleProgress * MAP_SCALE_INCREMENT)
                putInt(PreferenceKey.MAP_SCALE, mapScaleValue)

                // Save Map Fade
                val mapFadeProgress = seekBarMapFade.progress
                val mapFadeValue = MAP_FADE_MIN + (mapFadeProgress * MAP_FADE_INCREMENT)
                putInt(PreferenceKey.MAP_FADE, mapFadeValue)

                // Save Color Scheme
                val selectedColorSchemeId = radioGroupColorScheme.checkedRadioButtonId
                if (selectedColorSchemeId != -1) {
                    val selectedRadioButton = radioGroupColorScheme.findViewById<RadioButton>(selectedColorSchemeId)
                    putString(PreferenceKey.COLOR_SCHEME, selectedRadioButton.tag as String)
                }

            }

            // Notify the preference that its value might have changed
            // This is important if you want the summary of the DialogPreference to update
            val preference = preference
            if (preference is MapPreference) { // Assuming you create this class
                preference.callChangeListener(null) // Or pass the new values if needed
            }
        }
    }
}
