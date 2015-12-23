package org.blitzortung.android.data.provider.blitzortung

import android.text.format.DateFormat

import java.util.Calendar

class UrlFormatter {
    fun getUrlFor(type: BlitzortungHttpDataProvider.Type, region: Int, intervalTime: Calendar?, useGzipCompression: Boolean): String {

        val localPath: String

        if (type === BlitzortungHttpDataProvider.Type.STRIKES) {
            localPath = "Strokes/" + DateFormat.format("yyyy/MM/dd/kk/mm", intervalTime!!) + ".log"
        } else {
            localPath = type.name.toLowerCase() + ".txt"
        }

        val urlFormatString = "http://data.blitzortung.org/Data_%d/Protected/%s%s"
        return urlFormatString.format(region, localPath, if (useGzipCompression) ".gz" else "")
    }
}
