package org.blitzortung.android.data.provider;

import android.util.Log;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.data.beans.*;
import org.blitzortung.android.data.builder.DefaultStrokeBuilder;
import org.blitzortung.android.data.builder.StationBuilder;
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

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DATE_TIME_FORMATTER.setTimeZone(tz);
    }

    static private final String[] SERVERS = new String[]{"http://bo1.tryb.de:7080/", "http://bo2.tryb.de/"};

    static private int CURRENT_SERVER = 0;

    private final DefaultStrokeBuilder defaultStrokeBuilder;
    private final StationBuilder stationBuilder;

    private JsonRpcClient client;

    private int nextId = 0;

    private int[] histogram;

    private RasterParameters rasterParameters = null;

    private boolean incrementalResult;

    public JsonRpcDataProvider()
    {
        defaultStrokeBuilder = new DefaultStrokeBuilder();
        stationBuilder = new StationBuilder();
    }
    public List<AbstractStroke> getStrokes(int timeInterval, int intervalOffset, int region) {
        List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();
        rasterParameters = null;

        if (intervalOffset < 0) {
            nextId = 0;
        }
        incrementalResult = nextId != 0;

        try {
            JSONObject response = client.call("get_strokes", timeInterval, intervalOffset < 0 ? intervalOffset : nextId);

            readStrokes(response, strokes);
            readHistogramData(response);
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }

        Log.v(Main.LOG_TAG,
                String.format("JsonRpcDataProvider: read %d bytes (%d new strokes, region %d)", client.getLastNumberOfTransferredBytes(), strokes.size(), region));
        return strokes;
    }
    
    public boolean returnsIncrementalData()
    {
        return incrementalResult;
    }

    public List<AbstractStroke> getStrokesRaster(int intervalDuration, int intervalOffset, int rasterSize, int region) {
        List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();

        nextId = 0;
        incrementalResult = false;

        try {
            JSONObject response = client.call("get_strokes_raster", intervalDuration, rasterSize, intervalOffset, region);

            readRasterData(response, strokes);
            rasterParameters.setInfo(String.format("%.0f km", rasterSize / 1000f));
            readHistogramData(response);
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }

        Log.v(Main.LOG_TAG,
                String.format("JsonRpcDataProvider: read %d bytes (%d raster positions, region %d)", client.getLastNumberOfTransferredBytes(), strokes.size(), region));

        return strokes;
    }

    public int[] getHistogram() {
        return histogram;
    }

    public RasterParameters getRasterParameters() {
        return rasterParameters;
    }

    @Override
    public List<Station> getStations(int region) {
        List<Station> stations = new ArrayList<Station>();

        try {
            JSONObject response = client.call("get_stations");
            JSONArray stations_array = (JSONArray) response.get("stations");

            for (int i = 0; i < stations_array.length(); i++) {
                stations.add(stationBuilder.fromJson(stations_array.getJSONArray(i)));
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

    private void readStrokes(JSONObject response, List<AbstractStroke> strokes) throws JSONException {
        long referenceTimestamp = getReferenceTimestamp(response);
        JSONArray strokes_array = (JSONArray) response.get("s");
        for (int i = 0; i < strokes_array.length(); i++) {
            strokes.add(defaultStrokeBuilder.fromJson(referenceTimestamp, strokes_array.getJSONArray(i)));
        }
        if (response.has("next")) {
            nextId = (Integer) response.get("next");
        }
    }

    private void readRasterData(JSONObject response, List<AbstractStroke> strokes) throws JSONException {
        rasterParameters = new RasterParameters(response);
        long referenceTimestamp = getReferenceTimestamp(response);
        JSONArray strokes_array = (JSONArray) response.get("r");
        for (int i = 0; i < strokes_array.length(); i++) {
            strokes.add(new RasterElement(rasterParameters, referenceTimestamp, strokes_array.getJSONArray(i)));
        }
    }

    private long getReferenceTimestamp(JSONObject response) throws JSONException {
        return TimeFormat.parseTime(response.getString("t"));
    }

    private void readHistogramData(JSONObject response) throws JSONException {
        if (response.has("h")) {
            JSONArray histogram_array = (JSONArray) response.get("h");

            if (histogram == null || histogram.length != histogram_array.length()) {
                histogram = new int[histogram_array.length()];
            }

            for (int i = 0; i < histogram_array.length(); i++) {
                histogram[i] = histogram_array.getInt(i);
            }
        }
    }

    private static String getServer()
    {
        return SERVERS[CURRENT_SERVER];
    }

    private void skipServer()
    {
        CURRENT_SERVER = (CURRENT_SERVER + 1) % SERVERS.length;
    }
}
