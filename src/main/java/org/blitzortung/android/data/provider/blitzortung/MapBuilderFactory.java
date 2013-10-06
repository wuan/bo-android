package org.blitzortung.android.data.provider.blitzortung;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.builder.DefaultStrokeBuilder;
import org.blitzortung.android.data.builder.StationBuilder;
import org.blitzortung.android.util.TimeFormat;

import java.util.Map;

public class MapBuilderFactory {

    public MapBuilder<AbstractStroke> createAbstractStrokeMapBuilder() {
        return new MapBuilder<AbstractStroke>() {

            private final DefaultStrokeBuilder strokeBuilder = new DefaultStrokeBuilder();

            @Override
            protected void prepare(String[] fields) {
                strokeBuilder.init();

                strokeBuilder.setTimestamp(TimeFormat.parseTimestampWithMillisecondsFromFields(fields));
            }

            @Override
            protected void setBuilderMap(Map<String, Consumer> keyValueBuilderMap) {
                keyValueBuilderMap.put("pos", new Consumer(){
                    @Override
                    public void apply(String[] values) {
                        strokeBuilder.setLongitude(Float.parseFloat(values[0]));
                        strokeBuilder.setLatitude(Float.parseFloat(values[1]));
                        strokeBuilder.setAltitude(Integer.parseInt(values[2]));
                    }
                });
                keyValueBuilderMap.put("str", new Consumer(){
                    @Override
                    public void apply(String[] values) {
                        strokeBuilder.setAmplitude(Float.parseFloat(values[0]));
                    }
                });
                keyValueBuilderMap.put("dev", new Consumer(){
                    @Override
                    public void apply(String[] values) {
                        strokeBuilder.setLateralError(Integer.parseInt(values[0]));
                    }
                });
                keyValueBuilderMap.put("sta", new Consumer(){
                    @Override
                    public void apply(String[] values) {
                        strokeBuilder.setStationCount((short) values.length);
                    }
                });
            }

            @Override
            public AbstractStroke build() {
                return strokeBuilder.build();
            }
        };
    }

    public MapBuilder<Station> createStationMapBuilder() {
        return new MapBuilder<Station>() {

            private final StationBuilder stationBuilder = new StationBuilder();

            @Override
            protected void prepare(String[] fields) {
                stationBuilder.init();
            }

            @Override
            protected void setBuilderMap(Map<String, Consumer> keyValueBuilderMap) {
                keyValueBuilderMap.put("city", new Consumer(){
                    @Override
                    public void apply(String[] values) {
                        stationBuilder.setName(values[0]);
                    }
                });
                keyValueBuilderMap.put("pos", new Consumer(){
                    @Override
                    public void apply(String[] values) {
                        stationBuilder.setLongitude(Float.parseFloat(values[0]));
                        stationBuilder.setLatitude(Float.parseFloat(values[1]));
                    }
                });
                keyValueBuilderMap.put("last_signal", new Consumer(){
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
