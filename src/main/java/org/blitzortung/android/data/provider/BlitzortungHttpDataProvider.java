package org.blitzortung.android.data.provider;

import android.util.Log;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.provider.blitzortung.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class BlitzortungHttpDataProvider extends DataProvider {

    private UrlFormatter urlFormatter;

    private MapBuilder<AbstractStroke> strokeMapBuilder;
    private MapBuilder<Station> stationMapBuilder;
    private Splitter splitter;

    public enum Type {STROKES, STATIONS}

    private class MyAuthenticator extends Authenticator {

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

    private long latestTime = 0;

    public BlitzortungHttpDataProvider() {
        this(new UrlFormatter(), new Splitter(), new MapBuilderFactory());
    }

    public BlitzortungHttpDataProvider(UrlFormatter urlFormatter, Splitter splitter,
                                       MapBuilderFactory mapBuilderFactory) {
        this.urlFormatter = urlFormatter;
        this.splitter = splitter;
        strokeMapBuilder = mapBuilderFactory.createAbstractStrokeMapBuilder();
        stationMapBuilder = mapBuilderFactory.createStationMapBuilder();

    }

    @Override
    public List<AbstractStroke> getStrokes(int timeInterval, int intervalOffset, int region) {

        List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();

        TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar intervalTime = new GregorianCalendar(tz);

        if (username != null && username.length() != 0 && password != null && password.length() != 0) {

            try {
                IntervalTimer intervalTimer = new IntervalTimer(10 * 60 * 1000l);
                long startTime = System.currentTimeMillis() - timeInterval * 60 * 1000;

                intervalTimer.startInterval(Math.max(latestTime, startTime));

                while (intervalTimer.hasNext()) {
                    intervalTime.setTimeInMillis(intervalTimer.next());

                    BufferedReader reader = readFromUrl(Type.STROKES, region, intervalTime);
                    if (reader == null) {
                        continue;
                    }

                    int size = 0;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        size += line.length();

                        AbstractStroke stroke = strokeMapBuilder.buildFromLine(line);
                        long timestamp = stroke.getTimestamp();

                        if (timestamp > latestTime && timestamp >= startTime) {
                            strokes.add(stroke);
                        }
                    }
                    Log.v(Main.LOG_TAG,
                            String.format("BliztortungHttpDataProvider: read %d bytes (%d new strokes) from region %d", size, strokes.size(), region));

                    reader.close();
                }

                if (strokes.size() > 0) {
                    latestTime = strokes.get(strokes.size() - 1).getTimestamp();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new RuntimeException("no credentials provided");
        }

        return strokes;
    }

    public boolean returnsIncrementalData() {
        return latestTime != 0;
    }

    private BufferedReader readFromUrl(Type type, int region) {
        return readFromUrl(type, region, null);
    }

    private BufferedReader readFromUrl(Type type, int region, Calendar intervalTime) {

        boolean useGzipCompression = false;

        if (type == Type.STATIONS) {
            useGzipCompression = true;
        }

        Authenticator.setDefault(new MyAuthenticator());

        BufferedReader reader;

        String urlString = urlFormatter.getUrlFor(type, region, intervalTime, useGzipCompression);
        try {
            URL url;
            url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setAllowUserInteraction(false);
            InputStream ins = connection.getInputStream();
            if (useGzipCompression) {
                ins = new GZIPInputStream(ins);
            }
            reader = new BufferedReader(new InputStreamReader(ins));
        } catch (FileNotFoundException e) {
            Log.w(Main.LOG_TAG, String.format("URL '%s' not found", urlString));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return reader;
    }

    @Override
    public List<Station> getStations(int region) {
        List<Station> stations = new ArrayList<Station>();

        if (username != null && username.length() != 0 && password != null && password.length() != 0) {

            try {
                BufferedReader reader = readFromUrl(Type.STATIONS, region);

                int size = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    size += line.length();
                    try {
                        String[] fields = splitter.splitLine(line);
                        Station station = stationMapBuilder.buildFromFields(fields);
                        stations.add(station);
                    } catch (NumberFormatException e) {
                        Log.w(Main.LOG_TAG, String.format("BlitzortungHttpProvider: error parsing '%s'", line));
                    }
                }
                Log.v(Main.LOG_TAG,
                        String.format("BlitzortungHttpProvider: read %d bytes (%d stations) from region %d", size, stations.size(), region));

                reader.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new RuntimeException("no credentials provided");
        }

        return stations;
    }

    @Override
    public DataProviderType getType() {
        return DataProviderType.HTTP;
    }

    @Override
    public void setUp() {
    }

    @Override
    public void shutDown() {
    }

    @Override
    public int[] getHistogram() {
        return null;
    }

    @Override
    public RasterParameters getRasterParameters() {
        return null;
    }

    @Override
    public List<AbstractStroke> getStrokesRaster(int intervalDuration, int intervalOffset, int rasterSize, int region) {
        return null;
    }

    @Override
    public void reset() {
        latestTime = 0;
    }

    @Override
    public boolean isCapableOfHistoricalData() {
        return false;
    }

}
