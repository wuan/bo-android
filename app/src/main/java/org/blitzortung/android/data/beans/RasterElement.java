package org.blitzortung.android.data.beans;

import java.io.Serializable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper=true)
public class RasterElement extends StrikeAbstract implements Serializable {

    private static final long serialVersionUID = 6765788323616893614L;

    private final int multiplicity;

    @Builder
    public RasterElement(long timestamp, float longitude, float latitude, int multiplicity) {
        super(timestamp, longitude, latitude);
        this.multiplicity = multiplicity;
    }

    @Override
    public int getMultiplicity() {
        return multiplicity;
    }
}
