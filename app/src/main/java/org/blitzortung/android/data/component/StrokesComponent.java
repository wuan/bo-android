package org.blitzortung.android.data.component;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.map.color.ColorHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fj.F;
import fj.F2;
import fj.data.Array;
import fj.data.Java;

public class StrokesComponent {
    private Array<AbstractStroke> strokes;

    private int intervalDuration;
    private ColorHandler colorHandler;
    private RasterParameters rasterParameters;
    private int region;
    private long referenceTime;
    private int intervalOffset;

    public StrokesComponent()
    {
        strokes = Array.empty();
    }

    public void setIntervalDuration(int intervalDuration) {
        this.intervalDuration = intervalDuration;
    }

    public int getIntervalDuration() {
        return intervalDuration;
    }

    public ColorHandler getColorHandler() {
        return colorHandler;
    }

    public void setColorHandler(ColorHandler colorHandler) {
        this.colorHandler = colorHandler;
    }

    public void setRasterParameters(RasterParameters rasterParameters) {
        this.rasterParameters = rasterParameters;
    }

    public RasterParameters getRasterParameters() {
        return rasterParameters;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public int getRegion() {
        return region;
    }

    public void setReferenceTime(long referenceTime) {
        this.referenceTime = referenceTime;
    }

    public long getReferenceTime() {
        return referenceTime;
    }

    public void setIntervalOffset(int intervalOffset) {
        this.intervalOffset = intervalOffset;
    }

    public int getIntervalOffset() {
        return intervalOffset;
    }

    public void expireStrokes() {
        
    }

    public void clear() {
        strokes = Array.empty();
        referenceTime = 0;
        region = 0;
        rasterParameters = null;
    }

    public void addStrokes(Array<AbstractStroke> strokes) {
        this.strokes = this.strokes.append(strokes);
    }

    public Collection<AbstractStroke> getStrokes() {
        return strokes.toCollection();
    }

    public boolean hasRealtimeData() {
        return intervalOffset == 0;
    }

    public int getTotalNumberOfStrokes() {
        return strokes.foldLeft(new F2<Integer, AbstractStroke, Integer>() {
            @Override
            public Integer f(Integer integer, AbstractStroke abstractStroke) {
                return integer + abstractStroke.getMultiplicity();
            }
        }, Integer.valueOf(0));
    }
}
