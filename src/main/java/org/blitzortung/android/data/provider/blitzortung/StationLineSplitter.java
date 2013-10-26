package org.blitzortung.android.data.provider.blitzortung;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StationLineSplitter implements LineSplitter {

    @Override
    public String[] split(String text) {
        ArrayList<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("(\\w+(;(\"[^\"]+?\"|\\S+))+)");
        Matcher regexMatcher = regex.matcher(text);
        while (regexMatcher.find()) {
            if (regexMatcher.group(0) != null) {
                matchList.add(regexMatcher.group(1));
            }
        }
        return matchList.toArray(new String[matchList.size()]);
    }
}
