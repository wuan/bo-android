package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DataProviderTest {

    @Test
    public void testSetCredentials() {
        DataProvider dataProvider = new DataProviderForTest();

        dataProvider.setCredentials("foo", "bar");

        assertThat(dataProvider.username, is("foo"));
        assertThat(dataProvider.password, is("bar"));
    }

    class DataProviderForTest extends DataProvider {

        @Override
        public void setUp() {
        }

        @Override
        public void shutDown() {
        }

        @Override
        public void getStrikes(Parameters parameters, ResultEvent.ResultEventBuilder result) {
        }

        @Override
        public void getStrikesGrid(Parameters parameters, ResultEvent.ResultEventBuilder result) {
        }

        @Override
        public List<Station> getStations(int region) {
            return null;
        }

        @Override
        public DataProviderType getType() {
            return null;
        }

        @Override
        public void reset() {
        }

        @Override
        public boolean isCapableOfHistoricalData() {
            return false;
        }
    }
}
