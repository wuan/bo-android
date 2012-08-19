package org.blitzortung.android.data.provider;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Raster;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        public List<AbstractStroke> getStrokes(int timeInterval, int region) {
            return null;
        }

        @Override
        public List<AbstractStroke> getStrokesRaster(int timeInterval, int params, int timeOffet, int region) {
            return null;
        }

        @Override
        public Raster getRaster() {
            return null;
        }

        @Override
        public List<Participant> getStations(int region) {
            return null;
        }

        @Override
        public DataProviderType getType() {
            return null;
        }

        @Override
        public void reset() {
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
