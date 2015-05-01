package org.blitzortung.android.data.provider.blitzortung;

import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.provider.blitzortung.generic.LineSplitter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MapBuilderFactoryTest {

    @Mock
    private LineSplitter strikeLineSplitter;

    @Mock
    private LineSplitter stationLineSplitter;

    private MapBuilderFactory mapBuilderFactory;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        mapBuilderFactory = new MapBuilderFactory(strikeLineSplitter, stationLineSplitter);
    }

    @Test
    public void testStrikeBuilder() {
        MapBuilder<StrikeAbstract> strikeMapBuilder = mapBuilderFactory.createAbstractStrikeMapBuilder();

        when(strikeLineSplitter.split("<line>")).thenReturn(
                new String[]{
                        "2013-08-08",
                        "10:30:03.644038642",
                        "pos;44.162701;8.931001;0",
                        "str;4.75",
                        "dev;20146"
                }
        );

        StrikeAbstract strike = strikeMapBuilder.buildFromLine("<line>");

        assertThat(strike.getTimestamp(), is(1375957803644L));
        assertThat(strike.getLatitude(), is(44.162701f));
        assertThat(strike.getLongitude(), is(8.931001f));
        assertThat(strike.getMultiplicity(), is(1));
    }

    @Test
    public void testStationBuilder() {
        MapBuilder<Station> stationMapBuilder = mapBuilderFactory.createStationMapBuilder();

        when(stationLineSplitter.split("<line>")).thenReturn(
                new String[] {
                   "city;\"Egaldorf\"",
                   "pos;43.345542;11.465365;239",
                   "last_signal;\"2013-10-06 14:15:55\""
                }
        );

        Station station = stationMapBuilder.buildFromLine("<line>");

        assertThat(station.getName(), is("Egaldorf"));
        assertThat(station.getLatitude(), is(43.345542f));
        assertThat(station.getLongitude(), is(11.465365f));
        assertThat(station.getOfflineSince(), is(1381068955000L));
    }
}
