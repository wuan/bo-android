package org.blitzortung.android.data.provider

import android.content.SharedPreferences
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataEvent

class InvalidDataProvider : DataProvider {

    override fun getStrikes(parameters: Parameters): DataEvent {
        return createResultEvent(parameters).copy(failed = true)
    }

    override fun reset() {
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
    }
}