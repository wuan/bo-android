package org.blitzortung.android.data.provider.blitzortung;

import org.blitzortung.android.data.beans.DefaultStrike;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.provider.blitzortung.generic.Consumer;
import org.blitzortung.android.data.provider.blitzortung.generic.LineSplitter;
import org.blitzortung.android.util.TimeFormat;

import java.util.Map;

public class MapBuilderFactory {

    private final LineSplitter strikeLineSplitter;
    private final LineSplitter stationLineSplitter;

    public MapBuilderFactory() {
        this(text -> text.split(" "), new StationLineSplitter());
    }

    public MapBuilderFactory(LineSplitter strikeLineSplitter, LineSplitter stationLineSplitter) {
        this.strikeLineSplitter = strikeLineSplitter;
        this.stationLineSplitter = stationLineSplitter;
    }

    public MapBuilder<StrikeAbstract> createAbstractStrikeMapBuilder() {
        return new MapBuilder<StrikeAbstract>(strikeLineSplitter) {

            DefaultStrike.DefaultStrikeBuilder strikeBuilder = DefaultStrike.builder();

            @Override
            protected void prepare(String[] fields) {
                strikeBuilder.timestamp(TimeFormat.parseTimestampWithMillisecondsFromFields(fields));
            }

            @Override
            protected void setBuilderMap(Map<String, Consumer> keyValueBuilderMap) {
                keyValueBuilderMap.put("pos", values -> {
                    strikeBuilder.longitude(Float.parseFloat(values[1]));
                    strikeBuilder.latitude(Float.parseFloat(values[0]));
                    strikeBuilder.altitude(Integer.parseInt(values[2]));
                });
                keyValueBuilderMap.put("str", values -> strikeBuilder.amplitude(Float.parseFloat(values[0])));
                keyValueBuilderMap.put("dev", values -> strikeBuilder.lateralError(Integer.parseInt(values[0])));
                keyValueBuilderMap.put("sta", values -> strikeBuilder.stationCount((short) values.length));
            }

            @Override
            public StrikeAbstract build() {
                return strikeBuilder.build();
            }
        };
    }

    public MapBuilder<Station> createStationMapBuilder() {
        return new MapBuilder<Station>(stationLineSplitter) {

            private final Station.StationBuilder stationBuilder = Station.builder();

            @Override
            protected void prepare(String[] fields) {
                stationBuilder.name("");
            }

            @Override
            protected void setBuilderMap(Map<String, Consumer> keyValueBuilderMap) {
                keyValueBuilderMap.put("city", values -> stationBuilder.name(values[0].replace("\"", "")));
                keyValueBuilderMap.put("pos", values -> {
                    stationBuilder.longitude(Float.parseFloat(values[1]));
                    stationBuilder.latitude(Float.parseFloat(values[0]));
                });
                keyValueBuilderMap.put("last_signal", values -> {
                    String dateString = values[0].replace("\"", "").replace("-", "").replace(" ", "T");
                    stationBuilder.offlineSince(TimeFormat.parseTime(dateString));
                });
            }

            @Override
            protected Station build() {
                return stationBuilder.build();
            }
        };
    }
}
