package org.blitzortung.android.data.beans;

import java.io.Serializable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper=true)
public class DefaultStrike extends StrikeAbstract implements Serializable {

    private static final long serialVersionUID = 4201042078597105622L;

    private final int altitude;

    private final float amplitude;

    private final short stationCount;

    private final float lateralError;

    @Builder
    public DefaultStrike(long timestamp, float longitude, float latitude, int altitude, float amplitude, short stationCount, float lateralError) {
        super(timestamp, longitude, latitude);
        this.altitude = altitude;
        this.amplitude = amplitude;
        this.stationCount = stationCount;
        this.lateralError = lateralError;
    }
}
