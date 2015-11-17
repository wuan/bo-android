package org.blitzortung.android.data;

import com.annimon.stream.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ParametersControllerTest {

    private Parameters parameters;

    private ParametersController parametersController;

    @Before
    public void setUp() {
        parameters = Parameters.builder().intervalDuration(60).build();
        parametersController = ParametersController.withOffsetIncrement(15);
    }

    @Test
    public void testIfRealtimeModeIsDefault() {
        assertTrue(parameters.isRealtime());
    }

    @Test
    public void testGetOffsetReturnsZeroByDefault() {
        assertThat(parameters.getIntervalOffset(), is(0));
    }

    private boolean update(Function<Parameters, Parameters> updater) {
        Parameters oldParameters = parameters;
        parameters = parametersController.rewInterval(parameters);
        return !parameters.equals(oldParameters);
    }

    @Test
    public void testRewindInterval() {
        assertTrue(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-15));

        assertTrue(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-30));

        for (int i = 0; i < 23 * 4 - 2 - 1; i++) {
            assertTrue(update(parametersController::rewInterval));
        }
        assertThat(parameters.getIntervalOffset(), is(-23 * 60 + 15));

        assertTrue(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-23 * 60));

        assertFalse(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-23 * 60));
    }

    @Test
    public void testRewindIntervalWithAlignment() {
        parametersController = ParametersController.withOffsetIncrement(45);

        assertTrue(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-45));

        assertTrue(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-90));

        for (int i = 0; i < 23 / 3 * 4; i++) {
            assertTrue(update(parametersController::rewInterval));
        }
        assertThat(parameters.getIntervalOffset(), is(-23 * 60 + 30));

        assertFalse(update(parametersController::rewInterval));
        assertThat(parameters.getIntervalOffset(), is(-23 * 60 + 30));
    }

    @Test
    public void testFastforwardInterval() {
        update(parametersController::rewInterval);

        assertTrue(update(parametersController::ffwdInterval));
        assertThat(parameters.getIntervalOffset(), is(0));

        assertFalse(update(parametersController::ffwdInterval));
        assertThat(parameters.getIntervalOffset(), is(0));
    }

    @Test
    public void testGoRealtime() {
        assertFalse(update(parametersController::goRealtime));

        update(parametersController::rewInterval);

        assertTrue(update(parametersController::goRealtime));
        assertThat(parameters.getIntervalOffset(), is(0));
    }
}
