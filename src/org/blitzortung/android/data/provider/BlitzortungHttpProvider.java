package org.blitzortung.android.data.provider;

import java.util.List;

import org.blitzortung.android.data.Credentials;
import org.blitzortung.android.data.Stroke;

public class BlitzortungHttpProvider implements DataProvider {

	private Credentials creds;
	
	public BlitzortungHttpProvider(Credentials creds) {
		this.creds = creds;
	}
	
	@Override
	public List<Stroke> getStrokes() {
		// TODO Auto-generated method stub
		return null;
	}

}
