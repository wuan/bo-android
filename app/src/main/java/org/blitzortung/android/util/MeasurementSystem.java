package org.blitzortung.android.util;

public enum MeasurementSystem {
    METRIC("km", 1000.0f),
    IMPERIAL("mi.", 1609.344f);

    private final String unitName;

    private final float factor;

    private MeasurementSystem(String unitName, float factor) {
        this.unitName = unitName;
        this.factor = factor;
    }

    public String getUnitName() {
        return unitName;
    }

    public float calculateDistance(final float meters) {
        return meters / factor;
    }
}
