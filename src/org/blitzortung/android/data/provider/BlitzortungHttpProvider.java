package org.blitzortung.android.data.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

import android.util.Log;

public class BlitzortungHttpProvider extends DataProvider {

	class MyAuthenticator extends Authenticator {

		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

	private Date latestTime;

	@Override
	public List<AbstractStroke> getStrokes(int timeInterval, int region) {

		List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();

		if (username != null && username.length() != 0 && password != null && password.length() != 0) {

			Authenticator.setDefault(new MyAuthenticator());
			URL url;
			try {
				url = new URL(String.format("http://blitzortung.tmt.de/Data_%d/Protected/strikes.txt", region));
				URLConnection connection = url.openConnection();
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(10000);
				connection.setAllowUserInteraction(false);
				InputStream ins = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(ins));

				int size = 0;
				String line;
				while ((line = reader.readLine()) != null) {
					size += line.length();
					Stroke stroke = new Stroke(line);
					if (latestTime == null || stroke.getTime().after(latestTime))
						strokes.add(new Stroke(line));
				}
				Log.v("BlitzortungHttpProvider",
						String.format("read %d bytes (%d new strokes) from region %d", size, strokes.size(), region));

				if (strokes.size() > 0)
					latestTime = strokes.get(strokes.size() - 1).getTime();

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("no credentials provided");
		}

		return strokes;
	}

	@Override
	public List<Station> getStations() {
		return new ArrayList<Station>();
	}

	@Override
	public ProviderType getType() {
		return ProviderType.HTTP;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset() {
		latestTime = null;
	}

}
