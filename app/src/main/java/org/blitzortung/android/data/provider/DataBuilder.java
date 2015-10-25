package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.beans.DefaultStrike;
import org.blitzortung.android.data.beans.RasterElement;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.builder.StationBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataBuilder {

    private final DefaultStrike.DefaultStrikeBuilder defaultStrikeBuilder;
    private final StationBuilder stationBuilder;
    private final RasterParameters.RasterParametersBuilder rasterParametersBuilder;
    private final RasterElement.RasterElementBuilder rasterElementBuilder;

    public DataBuilder() {
        defaultStrikeBuilder = DefaultStrike.builder();
        stationBuilder = new StationBuilder();
        rasterParametersBuilder = RasterParameters.builder();
        rasterElementBuilder = RasterElement.builder();
    }

    public StrikeAbstract createDefaultStrike(long referenceTimestamp, JSONArray jsonArray) {
        try {
            return defaultStrikeBuilder
                    .timestamp(referenceTimestamp - 1000 * jsonArray.getInt(0))
                    .longitude((float) jsonArray.getDouble(1))
                    .latitude((float) jsonArray.getDouble(2))
                    .lateralError((float) jsonArray.getDouble(3))
                    .altitude(0)
                    .amplitude((float) jsonArray.getDouble(4))
                    .stationCount((short) jsonArray.getInt(5))
                    .build();
        } catch (JSONException e) {
            throw new IllegalStateException("error with JSON format while parsing strike data", e);
        }
    }

    public RasterParameters createRasterParameters(JSONObject response, String info) throws JSONException {
        return rasterParametersBuilder
                .longitudeStart((float) response.getDouble("x0"))
                .latitudeStart((float) response.getDouble("y1"))
                .longitudeDelta((float) response.getDouble("xd"))
                .latitudeDelta((float) response.getDouble("yd"))
                .longitudeBins(response.getInt("xc"))
                .latitudeBins(response.getInt("yc"))
                .info(info)
                .build();
    }

    public RasterElement createRasterElement(RasterParameters rasterParameters, long referenceTimestamp, JSONArray jsonArray) throws JSONException {
        return rasterElementBuilder
                .timestamp(referenceTimestamp + 1000 * jsonArray.getInt(3))
                .longitude(rasterParameters.getCenterLongitude(jsonArray.getInt(0)))
                .latitude(rasterParameters.getCenterLatitude(jsonArray.getInt(1)))
                .multiplicity(jsonArray.getInt(2))
                .build();
    }

    public Station createStation(JSONArray jsonArray) {
        return stationBuilder.fromJson(jsonArray);
    }
}
