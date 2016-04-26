/*

Blitzortung.org lightning monitor android app
Copyright (C) 2012 - 2015 Andreas Würl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.blitzortung.android.app

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.InvalidDataProvider
import org.blitzortung.android.data.provider.result.DataEvent
import rx.functions.Func1

class ParameterDataMapper(
        val preferences: SharedPreferences,
        val agentSuffix: String
) : Func1<Parameters, DataEvent>, OnSharedPreferenceChangeListener {

    var dataProvider: DataProvider = InvalidDataProvider()

    init {
        onSharedPreferencesChanged(preferences, PreferenceKey.DATA_SOURCE)
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun call(parameters: Parameters): DataEvent {
        Log.v(Main.LOG_TAG, "ParameterDataMapper.call($parameters)")

        return try {
            dataProvider.getStrikes(parameters)
        } catch (e: RuntimeException) {
            Log.e(Main.LOG_TAG, "error from data provider: $e")
            dataProvider.createResultEvent(parameters).copy(failed = true)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE -> {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(dataProvider)
                val dataProvider = DataProvider.createDataProvider(sharedPreferences, agentSuffix)
                sharedPreferences.registerOnSharedPreferenceChangeListener(dataProvider)
                this.dataProvider = dataProvider
            }
            else -> {}
        }
    }

}
