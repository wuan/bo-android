package org.blitzortung.android.alert.object;

import org.blitzortung.android.alert.AlertResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlertResultTest {

    @Mock
    private AlertSector alertSector;
    
    private String distanceUnitName = "km";

    private AlertResult alertResult;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(alertSector.getClosestStrokeDistance()).thenReturn(4567f);

        alertResult = new AlertResult(alertSector, distanceUnitName);
    }

    @Test
    public void testGetSector() throws Exception {
        when(alertSector.getLabel()).thenReturn("foo");
        assertThat(alertResult.getBearingName(), is("foo"));
    }

    @Test
    public void testGetDistance() throws Exception {
        assertThat(alertResult.getClosestStrokeDistance(), is(4567f));
    }
    
    @Test
    public void testGetDistanceUnit()
    {
        assertThat(alertResult.getDistanceUnitName(), is("km"));
    }
}
