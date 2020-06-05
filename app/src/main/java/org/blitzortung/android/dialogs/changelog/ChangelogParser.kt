package org.blitzortung.android.dialogs.changelog

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

class ChangelogParser {
    fun readChangeLog(context: Context, changeLogFileId: Int): List<Release> {
        val parser = getParser(context, changeLogFileId)

        return parseRoot(parser)
    }

    private fun parseRoot(parser: XmlPullParser) : List<Release> {
        val releases = mutableListOf<Release>()
        iterate(parser) {
            when (it.name) {
                "release" -> releases.add(parseRelease(parser))
            }
        }

        return releases
    }

    private fun iterate(parser: XmlPullParser, visitor: (parser: XmlPullParser) -> Unit) {
        while (parser.next() !in listOf(XmlPullParser.END_DOCUMENT, XmlPullParser.END_TAG)) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            visitor.invoke(parser)
        }
    }

    private fun parseRelease(parser: XmlPullParser) : Release {
        val changes = mutableListOf<Change>()
        val name = parser.name
        val versionName = parser.getAttributeValue(null, "versionName")
        val versionCode = parser.getAttributeValue(null, "versionCode").toInt()

        iterate(parser) {
            val name = it.name
            it.next()
            val text = it.text
            it.nextTag()
            val element = when (name) {
                "bugfix" -> Bugfix(text)
                "feature" -> Feature(text)
                "improvement" -> Improvement(text)
                else -> null
            }
            if (element != null) {
                changes.add(element)
            }
        }
        return Release(versionName, versionCode, changes)
    }

    private fun getParser(context: Context, changeLogFileId: Int): XmlPullParser {
        val resourceTypeName = context.resources.getResourceTypeName(changeLogFileId)
        return when (resourceTypeName) {
            "raw" -> {
                val inputStream = context.resources.openRawResource(changeLogFileId)
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)
                parser
            }
            "xml" -> context.resources.getXml(changeLogFileId)
            else -> throw IllegalArgumentException("bad changelog resource type $resourceTypeName")
        }
    }

}

data class Release(
        val versionName: String,
        val versionCode: Int,
        val changes: List<Change>
)

sealed class Change {
    abstract val description: String
}

data class Bugfix(
        override val description: String
) : Change()

data class Feature(
        override val description: String
) : Change()

data class Improvement(
        override val description: String
) : Change()
