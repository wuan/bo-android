package org.blitzortung.android.data;

public class ParametersController {

    private final static int MAX_RANGE = 24 * 60;

    private final int offsetIncrement;

    public ParametersController(final int offsetIncrement) {
        this.offsetIncrement = offsetIncrement;
    }

    public Parameters rewInterval(Parameters parameters) {
        return updateInterval(parameters, -offsetIncrement);
    }

    public Parameters ffwdInterval(Parameters parameters) {
        return updateInterval(parameters, offsetIncrement);
    }

    public Parameters updateInterval(Parameters parameters, int offsetIncrement) {
        int intervalOffset = parameters.getIntervalOffset() + offsetIncrement;
        final int intervalDuration = parameters.getIntervalDuration();

        if (intervalOffset < -MAX_RANGE + intervalDuration) {
            intervalOffset = -MAX_RANGE + intervalDuration;
        } else if (intervalOffset > 0) {
            intervalOffset = 0;
        }

        return parameters.createBuilder().intervalOffset(alignValue(intervalOffset)).build();
    }

    public Parameters goRealtime(Parameters parameters) {
        return parameters.createBuilder().intervalOffset(0).build();
    }

    private int alignValue(int value) {
        return (value / offsetIncrement) * offsetIncrement;
    }

}
