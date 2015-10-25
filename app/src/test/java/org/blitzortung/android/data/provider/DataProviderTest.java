package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.RasterParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DataProviderTest {

    class DataProviderForTest extends DataProvider {

        @Override
        public void setUp() {
        }

        @Override
        public void shutDown() {
        }

        @Override
        public List<StrikeAbstract> getStrikes(int intervalDuration, int intervalOffset, int region) {
            return null;
        }

        @Override
        public boolean returnsIncrementalData() {
            return false;
        }

        @Override
        public List<StrikeAbstract> getStrikesGrid(int intervalDuration, int intervalOffset, int rasterSize, int countThreshold, int region) {
            return null;
        }

        @Override
        public RasterParameters getRasterParameters() {
            return null;
        }

        @Override
        public int[] getHistogram() {
            return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
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

    @Test
    public void testSetCredentials() {
        DataProvider dataProvider = new DataProviderForTest();

        dataProvider.setCredentials("foo", "bar");

        assertThat(dataProvider.username, is("foo"));
        assertThat(dataProvider.password, is("bar"));
    }
}
