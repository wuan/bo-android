package org.blitzortung.android.data.provider.result;

import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.StrikeAbstract;

import java.util.ArrayList;
import java.util.List;

public class ResultEvent implements DataEvent {

    private final List<List<StrikeAbstract>> strikes;

    private List<Station> stations;

    private RasterParameters rasterParameters = null;

    private int[] histogram;

    private boolean fail;

    private boolean incrementalData;

    private long referenceTime;

    private Parameters parameters;

    public ResultEvent() {
        strikes = new ArrayList<>();
        fail = true;
        incrementalData = false;
    }

    public void setStrikes(List<StrikeAbstract> strikes) {
        this.strikes.clear();
        this.strikes.add(strikes);
        fail = false;
    }

    public boolean containsStrikes() {
        return !strikes.isEmpty();
    }

    public List<StrikeAbstract> getStrikes() {
        return strikes.get(0);
    }

    public void setStations(List<Station> stations) {
        this.stations = stations;
    }

    public boolean containsParticipants() {
        return stations != null;
    }

    public List<Station> getStations() {
        return stations;
    }

    public boolean hasFailed() {
        return fail;
    }

    public boolean hasRasterParameters() {
        return rasterParameters != null;
    }

    public RasterParameters getRasterParameters() {
        return rasterParameters;
    }

    public void setRasterParameters(RasterParameters rasterParameters) {
        this.rasterParameters = rasterParameters;
    }

    public boolean containsIncrementalData() {
        return incrementalData;
    }

    public void setContainsIncrementalData() {
        incrementalData = true;
    }

    public void setHistogram(int[] histogram) {
        this.histogram = histogram;
    }

    public int[] getHistogram() {
        return histogram;
    }

    public void setReferenceTime(long referenceTime) {
        this.referenceTime = referenceTime;
    }

    public long getReferenceTime() {
        return referenceTime;
    }

    public boolean containsRealtimeData() {
        return parameters != null && parameters.getIntervalOffset() == 0;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (fail) {
            sb.append("FailedResult()");
        } else {
            sb.append("Result(");
            final List<StrikeAbstract> currentStrikes = getStrikes();
            sb.append(currentStrikes != null ? currentStrikes.size() : 0).append(" strikes, ");
            sb.append(getParameters());
            if (hasRasterParameters()) {
                sb.append(", ").append(getRasterParameters());
            }
            sb.append(")");
        }

        return sb.toString();
    }
}
