package org.blitzortung.android.data.provider.blitzortung;

import org.blitzortung.android.data.beans.Station;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class MapBuilderFactoryTest {

    private MapBuilderFactory mapBuilderFactory;

    @Before
    public void setUp()
    {
        mapBuilderFactory = new MapBuilderFactory();
    }

    @Test
    public void testStationBuilder() {
        MapBuilder<Station> stationMapBuilder = mapBuilderFactory.createStationMapBuilder();
        String line = "station;10 user;11 city;\"Egaldorf\" country;\"Germany\" pos;43.345542;11.465365;239 board;6.6 firmware;\"WT 5.20.3 / 29A\" status;30 distance;71.474188743479 myblitz;N input_board;5.5;5.5;;;; input_firmware;\"29A\";\"29A\";\"\";\"\";\"\";\"\" input_gain;7.7;7.7;7.7;7.7;7.7;7.7 input_antenna;10;10;;;; last_signal;\"2013-10-06 14:15:55\" signals;217 last_stroke;\"2013-10-06 14:07:26\" strokes;0;0;0;0;1;0;31;504;6.15079\n";

        Splitter splitter = new Splitter();
        String[] fields = splitter.splitLine(line);
        Station station = stationMapBuilder.buildFromFields(fields);

        assertThat(station.getName(), is("Egaldorf"));
    }
}
