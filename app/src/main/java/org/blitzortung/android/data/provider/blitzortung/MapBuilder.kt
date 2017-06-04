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

import android.os.Build
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.util.isAtLeast
import java.lang.Exception
import java.util.*

abstract class MapBuilder<T> internal constructor(private val lineSplitter: (String) -> Array<String>) {

    private val keyValueBuilderMap: Map<String, (Array<String>) -> Unit>

    init {
        keyValueBuilderMap = HashMap<String, (Array<String>) -> Unit>()
        setBuilderMap(keyValueBuilderMap)
    }

    fun buildFromLine(line: String): T {
        val fields = lineSplitter.invoke(line)

        prepare(fields)

        for (field in fields) {
            val parts = field.split(SPLIT_PATTERN, 2).toTypedArray()
            if (parts.size > 1) {
                val key = parts[0]
                if (keyValueBuilderMap.containsKey(key)) {
                    val htmlString = parts[1]
                    val valueString = if (isAtLeast(Build.VERSION_CODES.N)) {
                        Html.fromHtml(htmlString, FROM_HTML_MODE_COMPACT).toString()
                    } else {
                        try {
                            Html.fromHtml(htmlString).toString()
                        } catch (throwable: Throwable) {
                            Log.w(Main.LOG_TAG, throwable)
                            break
                        }
                    }
                    val values = valueString.split(SPLIT_PATTERN).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val function: ((Array<String>) -> Unit)? = keyValueBuilderMap[key]
                    function?.invoke(values)
                }
            }
        }
        return build()
    }

    protected abstract fun prepare(fields: Array<String>)

    protected abstract fun setBuilderMap(keyValueBuilderMap: MutableMap<String, (Array<String>) -> Unit>)

    protected abstract fun build(): T

    companion object {
        val SPLIT_PATTERN = ";".toRegex()
    }
}
