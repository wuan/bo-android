package org.blitzortung.android.app;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class org.blitzortung.android.app.MainTest \
 * org.blitzortung.android.app.tests/android.test.InstrumentationTestRunner
 */
public class MainTest extends ActivityInstrumentationTestCase2<Main> {

    public MainTest() {
        super("org.blitzortung.android.app", Main.class);
    }

}
