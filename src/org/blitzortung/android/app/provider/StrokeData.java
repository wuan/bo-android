package org.blitzortung.android.app.provider;


import android.net.Uri;
import android.provider.BaseColumns;

public class StrokeData {
    public static final int ID_COLUMN = 0;
    public static final int TIMESTAMP_COLUMN = 1;
    public static final int LOCATION_COLUMN = 2;
    
    public static final String AUTHORITY =
            "org.blitzortung.android.provider.StrokeData";
    
    public static final class Strokes implements BaseColumns {
        private Strokes() {}
        
        public static final String STROKE = "stroke";
 
        public static final String TIMESTAMP_NAME = "timestamp";
        
        public static final String LOCATION_NAME = "location";
        
        public static final String QUERY_TEXT_NAME = "query_text";
        
        public static final Uri URI = Uri.parse("content://" +
                AUTHORITY + "/" + STROKE);
        
        public static final String QUERY_PARAM_NAME = "q";
    }
   

}
