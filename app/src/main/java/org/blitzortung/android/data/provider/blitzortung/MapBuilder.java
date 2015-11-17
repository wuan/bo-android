package org.blitzortung.android.data.provider.blitzortung;

import android.text.Html;

import org.blitzortung.android.data.provider.blitzortung.generic.Consumer;
import org.blitzortung.android.data.provider.blitzortung.generic.LineSplitter;

import java.util.HashMap;
import java.util.Map;

public abstract class MapBuilder<T> {

    private final Map<String, Consumer> keyValueBuilderMap;
    private final LineSplitter lineSplitter;

    MapBuilder(LineSplitter lineSplitter) {
        this.lineSplitter = lineSplitter;

        keyValueBuilderMap = new HashMap<>();

        setBuilderMap(keyValueBuilderMap);
    }

    public T buildFromLine(String line) {
        String[] fields = lineSplitter.split(line);

        prepare(fields);

        for (String field : fields) {
            String[] parts = field.split(";", 2);
            if (parts.length > 1) {
                String key = parts[0];
                if (keyValueBuilderMap.containsKey(key)) {
                    String[] values = Html.fromHtml(parts[1]).toString().split(";");
                    keyValueBuilderMap.get(key).apply(values);
                }
            }
        }
        return build();
    }

    protected abstract void prepare(String[] fields);

    protected abstract void setBuilderMap(Map<String, Consumer> keyValueBuilderMap);

    protected abstract T build();
}
