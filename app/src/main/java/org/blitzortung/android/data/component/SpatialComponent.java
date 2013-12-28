package org.blitzortung.android.data.component;

import org.blitzortung.android.data.beans.RasterParameters;

public class SpatialComponent {
    private final int region;
    private final int rasterBaselength;
    private final RasterParameters rasterParameters;

    public SpatialComponent(int region, int rasterBaselength, RasterParameters rasterParameters) {
        this.region = region;
        this.rasterBaselength = rasterBaselength;
        this.rasterParameters = rasterParameters;
    }
}
