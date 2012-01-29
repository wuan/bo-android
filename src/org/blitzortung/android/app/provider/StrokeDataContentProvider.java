package org.blitzortung.android.app.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class StrokeDataContentProvider extends ContentProvider {

	private final static int STROKES = 1;
	
	private static UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(StrokeData.AUTHORITY, StrokeData.Strokes.STROKE, STROKES);
	}
	
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String where, String[] whereArgs, String sortOrder) {
		Cursor c = null;
		
		int operation = URI_MATCHER.match(uri);
		
		switch(operation) {
		case STROKES:
			String queryText = uri.getQueryParameter(StrokeData.Strokes.QUERY_PARAM_NAME);
			
			if (queryText == null) {
				return null;
			}
			
			String select = StrokeData.Strokes.QUERY_TEXT_NAME + " = '" + queryText + "'";
			
			//c = blitzortung.query(STROKES_TABLE_NAME, projection, select, whereArgs, null, null, sortOrder);
			
			c.setNotificationUri(getContext().getContentResolver(), uri);
			
			if (!"".equals(queryText)) {
				//asyncQueryRequest(queryText, "asdf");
			}
		}
		
		return c;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return 0;
	}

}
