package org.blitzortung.android.data.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Stroke;

import android.util.Log;

public class BlitzortungHttpDataProvider extends DataProvider {
	
	private enum Type {STROKES, PARTICIPANTS};

	class MyAuthenticator extends Authenticator {

		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

	private long latestTime;

	@Override
	public List<AbstractStroke> getStrokes(int timeInterval, int region) {

		List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();

		if (username != null && username.length() != 0 && password != null && password.length() != 0) {

			try {
				BufferedReader reader = readFromUrl(Type.STROKES, region);

				int size = 0;
				String line;
				while ((line = reader.readLine()) != null) {
					size += line.length();
					Stroke stroke = new Stroke(line);
					if (stroke.getTimestamp() > latestTime) {
						strokes.add(new Stroke(line));
					}
				}
				Log.v("BlitzortungHttpProvider",
						String.format("read %d bytes (%d new strokes) from region %d", size, strokes.size(), region));

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

	private BufferedReader readFromUrl(Type type, int region) {

		Authenticator.setDefault(new MyAuthenticator());

		BufferedReader reader;

		try {
			URL url;
			url = new URL(String.format("http://blitzortung.net/Data_%d/Protected/%s.txt", region, type.name().toLowerCase()));
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(60000);
			connection.setReadTimeout(60000);
			connection.setAllowUserInteraction(false);
			InputStream ins = connection.getInputStream();
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
				BufferedReader reader = readFromUrl(Type.STROKES, region);

				int size = 0;
				String line;
				while ((line = reader.readLine()) != null) {
					size += line.length();
					Participant station = new Participant(line);
					stations.add(station);
				}
				Log.v("BlitzortungHttpProvider",
						String.format("read %d bytes (%d stations) from region %d", size, stations.size(), region));

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
	public Raster getRaster() {
		return null;
	}

	@Override
	public List<AbstractStroke> getStrokesRaster(int timeInterval, int rasterSize, int timeOffset, int region) {
		return null;
	}

	@Override
	public void reset() {
		latestTime = 0;
	}

}
