package org.blitzortung.android.data.provider.blitzortung;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class MapBuilderFactoryIntegrationTest {

    private MapBuilderFactory mapBuilderFactory;

    @Before
    public void setUp() {
        mapBuilderFactory = new MapBuilderFactory();
    }

    @Test
    public void testStrikeBuilder() {
        MapBuilder<StrikeAbstract> strikeMapBuilder = mapBuilderFactory.createAbstractStrikeMapBuilder();
        String line = "2013-08-08 10:30:03.644038642 pos;44.162701;8.931001;0 str;4.75 typ;0 dev;20146 sta;10;24;226,529,391,233,145,398,425,533,701,336,336,515,434,392,439,283,674,573,559,364,111,43,582,594\n";

        StrikeAbstract strike = strikeMapBuilder.buildFromLine(line);

        assertThat(strike.getTimestamp()).isEqualTo(1375957803644L);
        assertThat(strike.getLatitude()).isEqualTo(44.162701f);
        assertThat(strike.getLongitude()).isEqualTo(8.931001f);
        assertThat(strike.getMultiplicity()).isEqualTo(1);
    }

    @Test
    public void testStationBuilder() {
        MapBuilder<Station> stationMapBuilder = mapBuilderFactory.createStationMapBuilder();
        String line = "station;10 user;11 city;\"Egaldorf\" country;\"Germany\" pos;43.345542;11.465365;239 board;6.6 firmware;\"WT 5.20.3 / 29A\" status;30 distance;71.474188743479 myblitz;N input_board;5.5;5.5;;;; input_firmware;\"29A\";\"29A\";\"\";\"\";\"\";\"\" input_gain;7.7;7.7;7.7;7.7;7.7;7.7 input_antenna;10;10;;;; last_signal;\"2013-10-06 14:15:55\" signals;217 last_stroke;\"2013-10-06 14:07:26\" strokes;0;0;0;0;1;0;31;504;6.15079\n";

        Station station = stationMapBuilder.buildFromLine(line);

        assertThat(station.getName()).isEqualTo("Egaldorf");
        assertThat(station.getLatitude()).isEqualTo(43.345542f);
        assertThat(station.getLongitude()).isEqualTo(11.465365f);
        assertThat(station.getOfflineSince()).isEqualTo(1381068955000L);
    }
}
