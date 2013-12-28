package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.component.DataComponent;
import org.blitzortung.android.data.component.SpatialComponent;
import org.blitzortung.android.data.component.TimeComponent;

import java.io.Serializable;

public class DataResult implements Serializable {

	private static final long serialVersionUID = -2104015890700948020L;

    public static final DataResult PROCESS_LOCKED = new DataResult(null, null, null);

    private final DataComponent dataComponent;
    private final TimeComponent timeComponent;
    private final SpatialComponent spatialComponent;

    public DataResult(DataComponent dataComponent, TimeComponent timeComponent, SpatialComponent spatialComponent) {
        this.dataComponent = dataComponent;
        this.timeComponent = timeComponent;
        this.spatialComponent = spatialComponent;
    }

    public DataComponent getData()
    {
        return this.dataComponent;
    }

    public TimeComponent getTime()
    {
        return timeComponent;
    }

    public SpatialComponent getSpatial()
    {
        return spatialComponent;
    }

}
