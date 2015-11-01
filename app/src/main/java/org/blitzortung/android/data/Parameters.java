package org.blitzortung.android.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode
@ToString
@Builder
public class Parameters implements TimeIntervalWithOffset {

    public final static Parameters DEFAULT = Parameters.builder().region(-1).countThreshold(0).intervalDuration(0).intervalOffset(0).rasterBaselength(0).build();

    private int region;

    private int rasterBaselength;

    private int intervalDuration;

    private int intervalOffset;

    private int countThreshold;

    public boolean isRealtime() {
        return intervalOffset == 0;
    }

    public boolean isRaster() {
        return rasterBaselength != 0;
    }

    ParametersBuilder createBuilder() {
        return builder()
                .region(getRegion())
                .rasterBaselength(getRasterBaselength())
                .intervalDuration(getIntervalDuration())
                .intervalOffset(getIntervalOffset())
                .countThreshold(getCountThreshold());
    }
}
