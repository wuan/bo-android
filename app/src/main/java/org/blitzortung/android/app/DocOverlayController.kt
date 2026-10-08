/*

   Copyright 2026 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.edit
import org.blitzortung.android.app.databinding.DocOverlayBinding
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get

/**
 * Controls the quick documentation overlay that explains the main screen elements.
 *
 * The overlay is attached to [parent] and blocks the content behind it until the user
 * dismisses it. The dismissal is remembered via [PreferenceKey.DOC_OVERLAY_SHOWN] so the
 * overlay is only shown automatically once; afterwards it can be opened again on demand
 * from the main menu.
 */
class DocOverlayController(
    private val parent: ViewGroup,
    private val layoutInflater: LayoutInflater,
    private val preferences: SharedPreferences,
) {
    private var overlayBinding: DocOverlayBinding? = null

    val isShowing: Boolean
        get() = overlayBinding != null

    fun showIfNotShownBefore() {
        if (!hasBeenShownBefore()) {
            show()
        }
    }

    fun show() {
        if (overlayBinding != null) {
            return
        }
        val binding = DocOverlayBinding.inflate(layoutInflater, parent, false)
        binding.docButtonGotIt.setOnClickListener { dismissAndRemember() }
        parent.addView(binding.root)
        overlayBinding = binding
    }

    fun dismiss() {
        val binding = overlayBinding ?: return
        parent.removeView(binding.root)
        overlayBinding = null
    }

    fun dismissAndRemember() {
        dismiss()
        preferences.edit { putBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, true) }
    }

    fun hasBeenShownBefore(): Boolean = preferences.get(PreferenceKey.DOC_OVERLAY_SHOWN, false)
}
