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

package org.blitzortung.android.data.provider.blitzortung

import android.text.format.DateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlFormatter @Inject constructor() {
    fun getUrlFor(
        type: BlitzortungHttpDataProvider.Type,
        region: Int,
        intervalTime: Calendar?,
        useGzipCompression: Boolean
    ): String {

        val localPath: String

        localPath = if (type === BlitzortungHttpDataProvider.Type.STRIKES) {
            "Strokes/" + DateFormat.format("yyyy/MM/dd/kk/mm", intervalTime!!) + ".log"
        } else {
            type.name.lowercase(Locale.getDefault()) + ".txt"
        }

        val urlFormatString = "http://data.blitzortung.org/Data_%d/Protected/%s%s"
        return urlFormatString.format(region, localPath, if (useGzipCompression) ".gz" else "")
    }
}
