package org.blitzortung.android.data.provider.blitzortung;

import android.text.format.DateFormat;
import org.blitzortung.android.data.provider.BlitzortungHttpDataProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UrlFormatter {
    public String getUrlFor(BlitzortungHttpDataProvider.Type type, int region, Date intervalTime, boolean useGzipCompression) {

        String localPath;

        if (type == BlitzortungHttpDataProvider.Type.STROKES) {
            localPath = "Strokes/" + DateFormat.format("yyyy/MM/dd/kk/mm", intervalTime) + ".log";
        } else {
            localPath = type.name().toLowerCase() + ".txt";
        }

        String urlFormatString = "http://blitzortung.net/Data_%d/Protected/%s%s";
        return String.format(urlFormatString, region, localPath, useGzipCompression ? ".gz" : "");
    }
}
