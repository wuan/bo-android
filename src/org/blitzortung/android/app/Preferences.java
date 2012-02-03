package org.blitzortung.android.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
	
	private String username;
	private String password;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    addPreferencesFromResource(R.xml.preferences);
	    
	    getPrefs();
	}
	
    // A function to get the preferences    
    private void getPrefs() {
        // Gets data from the shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        username = prefs.getString("username", "");
        password = prefs.getString("password", "");
    }

}
