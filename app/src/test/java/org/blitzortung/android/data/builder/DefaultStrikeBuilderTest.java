package org.blitzortung.android.data.builder;

import org.blitzortung.android.data.beans.DefaultStrike;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DefaultStrikeBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private JSONArray jsonArray;

    private DefaultStrikeBuilder builder;
    private long referenceTimestamp;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        builder = new DefaultStrikeBuilder();
    }

    @Test
    public void testBuildFromJson() throws JSONException {
        referenceTimestamp = System.currentTimeMillis();

        when(jsonArray.getInt(0)).thenReturn(10);
        when(jsonArray.getDouble(1)).thenReturn(11.0);
        when(jsonArray.getDouble(2)).thenReturn(49.0);
        when(jsonArray.getDouble(3)).thenReturn(12.3);
        when(jsonArray.getDouble(4)).thenReturn(54.3);
        when(jsonArray.getInt(5)).thenReturn(6);
        when(jsonArray.getInt(6)).thenReturn(1);

        DefaultStrike strike = builder.fromJson(referenceTimestamp, jsonArray);

        assertThat(strike.getTimestamp()).isEqualTo(referenceTimestamp - 10 * 1000);
        assertThat(strike.getLongitude()).isEqualTo(11.0f);
        assertThat(strike.getLatitude()).isEqualTo(49.0f);
        assertThat(strike.getLateralError()).isEqualTo(12.3f);
        assertThat(strike.getAltitude()).isEqualTo(0);
        assertThat(strike.getAmplitude()).isEqualTo(54.3f);
        assertThat(strike.getStationCount()).isEqualTo((short) 6);

    }

    @Test
    public void testExceptionHandlingDuringConstruction() throws JSONException {
        when(jsonArray.getInt(0)).thenThrow(new JSONException("foo"));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("error with JSON format while parsing strike data");

        builder.fromJson(referenceTimestamp, jsonArray);
    }

}
