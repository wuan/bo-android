package org.blitzortung.android.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {
	
	public static final String USERNAME_KEY = "username";
	public static final String PASSWORD_KEY = "password";
	public static final String RASTER_SIZE_KEY = "raster_size";
	final static String MAP_TYPE_KEY = "map_mode";
	final static String PERIOD_KEY = "period";

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    addPreferencesFromResource(R.xml.preferences);
	}

}
