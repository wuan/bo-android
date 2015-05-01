package org.blitzortung.android.data.provider.blitzortung;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(RobolectricTestRunner.class)
public class UrlFormatterTest {

    private UrlFormatter urlFormatter;

    @Before
    public void setUp() throws Exception {
        urlFormatter = new UrlFormatter();
    }

    @Test
    public void testGetUrlForStations() throws Exception {

        assertThat(urlFormatter.getUrlFor(BlitzortungHttpDataProvider.Type.STATIONS, 1, null, false)).isEqualTo("http://data.blitzortung.org/Data_1/Protected/stations.txt");

        assertThat(urlFormatter.getUrlFor(BlitzortungHttpDataProvider.Type.STATIONS, 2, null, true)).isEqualTo("http://data.blitzortung.org/Data_2/Protected/stations.txt.gz");
    }

    @Test
    public void testGetUrlForStrikes() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar date = new GregorianCalendar(tz);
        date.setTimeInMillis(1381065000000l);

        assertThat(urlFormatter.getUrlFor(BlitzortungHttpDataProvider.Type.STRIKES, 1, date, false)).isEqualTo("http://data.blitzortung.org/Data_1/Protected/Strokes/2013/10/06/13/10.log");
    }

}
