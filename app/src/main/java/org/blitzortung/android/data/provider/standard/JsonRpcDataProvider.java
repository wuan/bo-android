package org.blitzortung.android.data.provider.standard;

import android.util.Log;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.provider.DataBuilder;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataProviderType;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.blitzortung.android.jsonrpc.JsonRpcClient;
import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class JsonRpcDataProvider extends DataProvider {

    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    static private final String[] SERVERS = new String[]{"http://bo-service.tryb.de:7080/", "http://bo-service.tryb.de/"};
    static private int CURRENT_SERVER = 0;

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DATE_TIME_FORMATTER.setTimeZone(tz);
    }

    private final DataBuilder dataBuilder;
    private JsonRpcClient client;
    private int nextId = 0;
    private boolean incrementalResult;

    public JsonRpcDataProvider() {
        dataBuilder = new DataBuilder();
    }

    private static String getServer() {
        return SERVERS[CURRENT_SERVER];
    }

    @Override
    public void getStrikes(Parameters parameters, ResultEvent.ResultEventBuilder result) {
        final int intervalDuration = parameters.getIntervalDuration();
        final int intervalOffset = parameters.getIntervalOffset();
        if (intervalOffset < 0) {
            nextId = 0;
        }
        result.incrementalData(nextId != 0);

        final int newStrikeCount;
        try {
            JSONObject response = client.call("get_strikes", intervalDuration, intervalOffset < 0 ? intervalOffset : nextId);

            newStrikeCount = addStrikes(response, result);
            addStrikesHistogram(response, result);
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }

        Log.v(Main.LOG_TAG,
                String.format("JsonRpcDataProvider: read %d bytes (%d new strikes)", client.getLastNumberOfTransferredBytes(), newStrikeCount));
    }

    public boolean returnsIncrementalData() {
        return incrementalResult;
    }

    @Override
    public void getStrikesGrid(Parameters parameters, ResultEvent.ResultEventBuilder result) {
        nextId = 0;
        incrementalResult = false;

        final int intervalDuration = parameters.getIntervalDuration();
        final int intervalOffset = parameters.getIntervalOffset();
        final int rasterBaselength = parameters.getRasterBaselength();
        final int countThreshold = parameters.getCountThreshold();
        final int region = parameters.getRegion();

        final int elementCount;
        try {
            JSONObject response = client.call("get_strikes_grid", intervalDuration, rasterBaselength, intervalOffset, region, countThreshold);

            String info = String.format("%.0f km", rasterBaselength / 1000f);
            elementCount = addRasterData(response, result, info);
            addStrikesHistogram(response, result);
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }

        Log.v(Main.LOG_TAG,
                String.format("JsonRpcDataProvider: read %d bytes (%d raster positions, region %d)", client.getLastNumberOfTransferredBytes(), elementCount, region));
    }

    @Override
    public List<Station> getStations(int region) {
        List<Station> stations = new ArrayList<>();

        try {
            JSONObject response = client.call("get_stations");
            JSONArray stations_array = (JSONArray) response.get("stations");

            for (int i = 0; i < stations_array.length(); i++) {
                stations.add(dataBuilder.createStation(stations_array.getJSONArray(i)));
            }
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }
        return stations;
    }

    @Override
    public DataProviderType getType() {
        return DataProviderType.RPC;
    }

    @Override
    public void setUp() {
        String agentSuffix = pInfo != null ? "-" + Integer.toString(pInfo.versionCode) : "";
        client = new JsonRpcClient(getServer(), agentSuffix);
        client.setConnectionTimeout(40000);
        client.setSocketTimeout(40000);
    }

    @Override
    public void shutDown() {
        client.shutdown();
        client = null;
    }

    @Override
    public void reset() {
        nextId = 0;
    }

    @Override
    public boolean isCapableOfHistoricalData() {
        return true;
    }

    private int addStrikes(JSONObject response, ResultEvent.ResultEventBuilder result) throws JSONException {
        final List<StrikeAbstract> strikes = new ArrayList<>();
        long referenceTimestamp = getReferenceTimestamp(response);
        JSONArray strikes_array = (JSONArray) response.get("s");
        for (int i = 0; i < strikes_array.length(); i++) {
            strikes.add(dataBuilder.createDefaultStrike(referenceTimestamp, strikes_array.getJSONArray(i)));
        }
        if (response.has("next")) {
            nextId = (Integer) response.get("next");
        }
        result.strikes(strikes);
        return strikes.size();
    }

    private int addRasterData(JSONObject response, ResultEvent.ResultEventBuilder result, String info) throws JSONException {
        final RasterParameters rasterParameters = dataBuilder.createRasterParameters(response, info);
        long referenceTimestamp = getReferenceTimestamp(response);

        JSONArray strikes_array = (JSONArray) response.get("r");
        final List<StrikeAbstract> strikes = new ArrayList<>();
        for (int i = 0; i < strikes_array.length(); i++) {
            strikes.add(dataBuilder.createRasterElement(rasterParameters, referenceTimestamp, strikes_array.getJSONArray(i)));
        }

        result.rasterParameters(rasterParameters);
        result.strikes(strikes);

        return strikes.size();
    }

    private long getReferenceTimestamp(JSONObject response) throws JSONException {
        return TimeFormat.parseTime(response.getString("t"));
    }

    private void addStrikesHistogram(JSONObject response, ResultEvent.ResultEventBuilder result) throws JSONException {
        if (response.has("h")) {
            JSONArray histogram_array = (JSONArray) response.get("h");

            final int[] histogram = new int[histogram_array.length()];

            for (int i = 0; i < histogram_array.length(); i++) {
                histogram[i] = histogram_array.getInt(i);
            }
            result.histogram(histogram);
        }
    }

    private void skipServer() {
        CURRENT_SERVER = (CURRENT_SERVER + 1) % SERVERS.length;
    }
}
