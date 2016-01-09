package org.blitzortung.android.data.provider.blitzortung;

import android.text.format.DateFormat;

import java.util.Calendar;

public class UrlFormatter {
    public String getUrlFor(BlitzortungHttpDataProvider.Type type, int region, Calendar intervalTime, boolean useGzipCompression) {

        String localPath;

        if (type == BlitzortungHttpDataProvider.Type.STROKES) {
            localPath = "Strokes/" + DateFormat.format("yyyy/MM/dd/kk/mm", intervalTime) + ".log";
        } else {
            localPath = type.name().toLowerCase() + ".txt";
        }

        String urlFormatString = "http://data.blitzortung.org/Data_%d/Protected/%s%s";
        return String.format(urlFormatString, region, localPath, useGzipCompression ? ".gz" : "");
    }
}
