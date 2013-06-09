package org.blitzortung.android.data.provider;

import android.util.Log;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.DefaultStroke;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.RasterParameters;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.blitzortung.android.util.TimeFormat.parseTimestampWithMillisecondsFromFields;

public class BlitzortungHttpDataProvider extends DataProvider {
	
	private enum Type {STRIKES, STATIONS}

	private class MyAuthenticator extends Authenticator {

		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

	private long latestTime = 0;

	@Override
	public List<AbstractStroke> getStrokes(int timeInterval, int intervalOffset, int region) {

		List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();

		if (username != null && username.length() != 0 && password != null && password.length() != 0) {

			try {
                long startTime = System.currentTimeMillis() - timeInterval * 60 * 1000;
				BufferedReader reader = readFromUrl(Type.STRIKES, region);

				int size = 0;
				String line;
				while ((line = reader.readLine()) != null) {
					size += line.length();
                    String[] fields = line.split(" ");
                    long parsedTimestamp = parseTimestampWithMillisecondsFromFields(fields);

                    if (parsedTimestamp > latestTime && parsedTimestamp >= startTime) {
						strokes.add(new DefaultStroke(parsedTimestamp, fields));
					}
				}
				Log.v(Main.LOG_TAG,
						String.format("BliztortungHttpDataProvider: read %d bytes (%d new strokes) from region %d", size, strokes.size(), region));

				if (strokes.size() > 0)
					latestTime = strokes.get(strokes.size() - 1).getTimestamp();

				reader.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		} else {
			throw new RuntimeException("no credentials provided");
		}

		return strokes;
	}

    public boolean returnsIncrementalData()
    {
        return latestTime != 0;
    }

	private BufferedReader readFromUrl(Type type, int region) {

		boolean useGzipCompression = region == 1;
		
		Authenticator.setDefault(new MyAuthenticator());

		BufferedReader reader;

		try {
			URL url;
			url = new URL(String.format("http://blitzortung.net/Data_%d/Protected/%s.txt%s", region, type.name().toLowerCase(), useGzipCompression ? ".gz" : ""));
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(60000);
			connection.setReadTimeout(60000);
			connection.setAllowUserInteraction(false);
			InputStream ins = connection.getInputStream();
			if (useGzipCompression) {
				ins = new GZIPInputStream(ins);
			}
			reader = new BufferedReader(new InputStreamReader(ins));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return reader;
	}

	@Override
	public List<Participant> getStations(int region) {
		List<Participant> stations = new ArrayList<Participant>();

		if (username != null && username.length() != 0 && password != null && password.length() != 0) {

			try {
				BufferedReader reader = readFromUrl(Type.STATIONS, region);

				int size = 0;
				String line;
				while ((line = reader.readLine()) != null) {
					size += line.length();
                    try {
					Participant station = new Participant(line);
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
