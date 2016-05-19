/*

   Copyright 2015 Andreas Würl

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

package org.blitzortung.android.data.provider

import android.content.SharedPreferences
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.provider.result.ResultEvent

abstract class DataProvider(
        protected val preferences : SharedPreferences,
        vararg keys: PreferenceKey
) : OnSharedPreferenceChangeListener {

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferencesChanged(preferences, *keys)
    }

    abstract val type: DataProviderType

    abstract fun reset()

    abstract fun <T> retrieveData(retrieve: DataRetriever.() -> T): T

    fun unregister() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    interface DataRetriever {
        fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent
        fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent
        fun getStations(region: Int): List<Station>
    }
}
