package org.blitzortung.android.data.provider.blitzortung

import android.text.Html

import java.util.HashMap

abstract class MapBuilder<T> internal constructor(private val lineSplitter: (String) -> Array<String>) {

    private val keyValueBuilderMap: Map<String, (Array<String>) -> Unit>

    init {
        keyValueBuilderMap = HashMap<String, (Array<String>) -> Unit>()
    }

    fun buildFromLine(line: String): T {
        val fields = lineSplitter.invoke(line)

        prepare(fields)

        for (field in fields) {
            val parts = field.split(";".toRegex(), 2).toTypedArray()
            if (parts.size > 1) {
                val key = parts[0]
                if (keyValueBuilderMap.containsKey(key)) {
                    val values = Html.fromHtml(parts[1]).toString().split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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
}
