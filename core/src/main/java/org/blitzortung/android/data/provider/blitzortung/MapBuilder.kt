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

import android.util.Log
import androidx.core.text.HtmlCompat
import org.blitzortung.android.util.LogUtil.LOG_TAG

abstract class MapBuilder<T> internal constructor(private val lineSplitter: (String) -> Array<String>) {
    protected val keyValueBuilderMap: HashMap<String, (Array<String>) -> Unit> = HashMap()

    fun buildFromLine(line: String): T? {
        return try {
            buildFromLineChecked(line)
        } catch (e: MapBuilderFailedException) {
            null
        }
    }

    private fun buildFromLineChecked(line: String): T {
        val fields = lineSplitter.invoke(line)

        prepare(fields)

        for (field in fields) {
            val parts = field.split(SPLIT_PATTERN, 2).toTypedArray()
            if (parts.size > 1) {
                val key = parts[0]
                if (keyValueBuilderMap.containsKey(key)) {
                    val values = extractValues(parts[1]) ?: break
                    val function: ((Array<String>) -> Unit)? = keyValueBuilderMap[key]
                    function?.invoke(values)
                }
            }
        }
        return build()
    }

    private fun extractValues(htmlString: String): Array<String>? {
        @Suppress("TooGenericExceptionCaught")
        val valueString =
            try {
                HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
            } catch (e: RuntimeException) {
                // detekt: on API < 24 HtmlCompat delegates to Html.fromHtml, which has no specific
                // exception type; a malformed tag can throw any RuntimeException, so keep the broad catch.
                Log.w(LOG_TAG, e)
                return null
            }
        return valueString.split(SPLIT_PATTERN).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    protected abstract fun prepare(fields: Array<String>)

    protected abstract fun build(): T

    companion object {
        val SPLIT_PATTERN = ";".toRegex()
    }
}
