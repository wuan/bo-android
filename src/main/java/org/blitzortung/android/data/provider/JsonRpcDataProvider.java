package org.blitzortung.android.data.provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.RasterElement;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.jsonrpc.JsonRpcClient;
import org.blitzortung.android.util.TimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonRpcDataProvider extends DataProvider {

    public static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DATE_TIME_FORMATTER.setTimeZone(tz);
    }

    static private final String[] SERVERS = new String[]{"http://bo1.tryb.de:7080/", "http://bo2.tryb.de/"};
    static private int CURRENT_SERVER = 0;

    private JsonRpcClient client;

    private Integer nextId = null;

    private int[] histogram;

    Raster raster = null;

    public List<AbstractStroke> getStrokes(int timeInterval, int region) {
        List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();
        raster = null;

        try {
            JSONObject response = client.call("get_strokes", timeInterval, nextId);

            readStrokes(response, strokes);
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }
        return strokes;
    }

    public List<AbstractStroke> getStrokesRaster(int timeInterval, int rasterSize, int timeOffset, int region) {
        List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();

        nextId = null;

        try {
            JSONObject response = client.call("get_strokes_raster", timeInterval, rasterSize, timeOffset, region);

            readRasterData(response, strokes);
            readHistogramData(response);
        } catch (Exception e) {
            skipServer();
            throw new RuntimeException(e);
        }

        return strokes;
    }

    public int[] getHistogram() {
        return histogram;
    }

    public Raster getRaster() {
        return raster;
    }

    @Override
    public List<Participant> getStations(int region) {
        List<Participant> stations = new ArrayList<Participant>();

        try {
            JSONObject response = client.call("get_stations");
            JSONArray stations_array = (JSONArray) response.get("stations");

            for (int i = 0; i < stations_array.length(); i++) {
                stations.add(new Participant(stations_array.getJSONArray(i)));
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
        client = new JsonRpcClient(getServer());
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
        nextId = null;
    }

    private void readStrokes(JSONObject response, List<AbstractStroke> strokes) throws JSONException {
        long referenceTimestamp = getReferenceTimestamp(response);
        JSONArray strokes_array = (JSONArray) response.get("s");
        for (int i = 0; i < strokes_array.length(); i++) {
            strokes.add(new Stroke(referenceTimestamp, strokes_array.getJSONArray(i)));
        }
        if (response.has("next")) {
            nextId = (Integer) response.get("next");
        }
    }

    private void readRasterData(JSONObject response, List<AbstractStroke> strokes) throws JSONException {
        raster = new Raster(response);
        long referenceTimestamp = getReferenceTimestamp(response);
        JSONArray strokes_array = (JSONArray) response.get("r");
        for (int i = 0; i < strokes_array.length(); i++) {
            strokes.add(new RasterElement(raster, referenceTimestamp, strokes_array.getJSONArray(i)));
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
