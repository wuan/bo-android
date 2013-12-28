package org.blitzortung.android.data.provider.blitzortung;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Splitter {

    public String[] splitLine(String line) {
        ArrayList<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("(\\w+(;(\"[^\"]+?\"|\\S+))+)");
        Matcher regexMatcher = regex.matcher(line);
        while (regexMatcher.find()) {
            if (regexMatcher.group(0) != null) {
                matchList.add(regexMatcher.group(1));
            }
        }
        return matchList.toArray(new String[matchList.size()]);
    }
}
