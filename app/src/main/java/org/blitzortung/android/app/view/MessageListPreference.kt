package org.blitzortung.android.app.view // Adjust package as needed

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.blitzortung.android.app.R // Make sure this R import is correct for your project

class MessageListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreference(context, attrs) {

    init {
        dialogLayoutResource = R.layout.custom_list_preference_dialog_layout
    }
}
