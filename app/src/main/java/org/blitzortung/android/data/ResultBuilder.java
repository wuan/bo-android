package org.blitzortung.android.data;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.component.DataComponent;
import org.blitzortung.android.data.component.SpatialComponent;
import org.blitzortung.android.data.component.TimeComponent;
import org.blitzortung.android.data.provider.DataResult;

import fj.data.Array;

public class ResultBuilder {
    private Array<AbstractStroke> strokes;
    private Array<Station> participants;
    private int[] histogram;
    private int intervalDuration;
    private int intervalOffset;
    private long referenceTime;
    private boolean incremental;
    private int region;
    private int rasterBaselength;
    private RasterParameters rasterParameters;

    public ResultBuilder withStrokes(Array<AbstractStroke> strokes) {
        this.strokes = strokes;
        return this;
    }

    public ResultBuilder withIntervalDuration(int intervalDuration) {
        this.intervalDuration = intervalDuration;
        return this;
    }

    public ResultBuilder withIntervalOffset(int intervalOffset) {
        this.intervalOffset = intervalOffset;
        return this;
    }

    public ResultBuilder withRegion(int region) {
        this.region = region;
        return this;
    }

    public ResultBuilder withRasterBaselength(int rasterBaselength) {
        this.rasterBaselength = rasterBaselength;
        return this;
    }

    public ResultBuilder isIncremental(boolean incremental) {
        this.incremental = incremental;
        return this;
    }

    public ResultBuilder withReferenceTime(long referenceTime) {
        this.referenceTime = referenceTime;
        return this;
    }

    public ResultBuilder withRasterParameters(RasterParameters rasterParameters) {
        this.rasterParameters = rasterParameters;
        return this;
    }

    public ResultBuilder withHistogram(int[] histogram) {
        this.histogram = histogram;
        return this;
    }

    public ResultBuilder withParticipants(Array<Station> participants) {
        this.participants = participants;
        return this;
    }

    public DataResult build() {
        DataComponent dataComponent = new DataComponent(strokes, participants, histogram);
        TimeComponent timeComponent = new TimeComponent(intervalDuration, intervalOffset, referenceTime, incremental);
        SpatialComponent spatialComponent = new SpatialComponent(region, rasterBaselength, rasterParameters);
        return new DataResult(dataComponent, timeComponent, spatialComponent);
    }
}
