package org.blitzortung.android.data.provider.blitzortung;

import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.builder.DefaultStrikeBuilder;
import org.blitzortung.android.data.builder.StationBuilder;
import org.blitzortung.android.data.provider.blitzortung.generic.Consumer;
import org.blitzortung.android.data.provider.blitzortung.generic.LineSplitter;
import org.blitzortung.android.util.TimeFormat;

import java.util.Map;

public class MapBuilderFactory {

    private final LineSplitter strikeLineSplitter;
    private final LineSplitter stationLineSplitter;

    public MapBuilderFactory() {
        this(new LineSplitter() {
            @Override
            public String[] split(String text) {
                return text.split(" ");
            }
        }, new StationLineSplitter());
    }

    public MapBuilderFactory(LineSplitter strikeLineSplitter, LineSplitter stationLineSplitter) {
        this.strikeLineSplitter = strikeLineSplitter;
        this.stationLineSplitter = stationLineSplitter;
    }

    public MapBuilder<StrikeAbstract> createAbstractStrikeMapBuilder() {
        return new MapBuilder<StrikeAbstract>(strikeLineSplitter) {

            private final DefaultStrikeBuilder strikeBuilder = new DefaultStrikeBuilder();

            @Override
            protected void prepare(String[] fields) {
                strikeBuilder.init();

                strikeBuilder.setTimestamp(TimeFormat.parseTimestampWithMillisecondsFromFields(fields));
            }

            @Override
            protected void setBuilderMap(Map<String, Consumer> keyValueBuilderMap) {
                keyValueBuilderMap.put("pos", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        strikeBuilder.setLongitude(Float.parseFloat(values[1]));
                        strikeBuilder.setLatitude(Float.parseFloat(values[0]));
                        strikeBuilder.setAltitude(Integer.parseInt(values[2]));
                    }
                });
                keyValueBuilderMap.put("str", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        strikeBuilder.setAmplitude(Float.parseFloat(values[0]));
                    }
                });
                keyValueBuilderMap.put("dev", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        strikeBuilder.setLateralError(Integer.parseInt(values[0]));
                    }
                });
                keyValueBuilderMap.put("sta", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        strikeBuilder.setStationCount((short) values.length);
                    }
                });
            }

            @Override
            public StrikeAbstract build() {
                return strikeBuilder.build();
            }
        };
    }

    public MapBuilder<Station> createStationMapBuilder() {
        return new MapBuilder<Station>(stationLineSplitter) {

            private final StationBuilder stationBuilder = new StationBuilder();

            @Override
            protected void prepare(String[] fields) {
                stationBuilder.init();
            }

            @Override
            protected void setBuilderMap(Map<String, Consumer> keyValueBuilderMap) {
                keyValueBuilderMap.put("city", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        stationBuilder.setName(values[0].replace("\"", ""));
                    }
                });
                keyValueBuilderMap.put("pos", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        stationBuilder.setLongitude(Float.parseFloat(values[1]));
                        stationBuilder.setLatitude(Float.parseFloat(values[0]));
                    }
                });
                keyValueBuilderMap.put("last_signal", new Consumer() {
                    @Override
                    public void apply(String[] values) {
                        String dateString = values[0].replace("\"", "").replace("-", "").replace(" ", "T");
                        stationBuilder.setOfflineSince(TimeFormat.parseTime(dateString));
                    }
                });
            }

            @Override
            protected Station build() {
                return stationBuilder.build();
            }
        };
    }
}
