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

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

public class BlitzortungHttpProvider extends DataProvider {
	
	class MyAuthenticator extends Authenticator {

		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

	private Date latestTime;
	
	//private int latestNanoseconds;
	
	@Override
	public DataResult<Stroke> getStrokes(int timeInterval) {

		DataResult<Stroke> dataResult = new DataResult<Stroke>();

		if (username != null && password != null) {

			Authenticator.setDefault(new MyAuthenticator());
			URL url;
			try {
				url = new URL("http://blitzortung.tmt.de/Data/Protected/strikes.txt");
				URLConnection connection = url.openConnection();
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(10000);
				connection.setAllowUserInteraction(false);
				InputStream ins = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
				
				List<Stroke> strokes = new ArrayList<Stroke>();
				
				String line;
				while ((line = reader.readLine()) != null) {
					Stroke stroke = new Stroke(line);
					if (latestTime == null || stroke.getTime().after(latestTime))
					  strokes.add(new Stroke(line));
				}
				
				if (strokes.size() > 0)
					latestTime = strokes.get(strokes.size()-1).getTime();
				
				dataResult.setData(strokes);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return dataResult;
	}

	@Override
	public DataResult<Station> getStations() {
		return new DataResult<Station>();
	}

	@Override
	public ProviderType getType() {
		return ProviderType.HTTP;
	}

}
