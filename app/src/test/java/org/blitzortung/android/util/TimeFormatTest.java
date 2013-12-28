package org.blitzortung.android.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TimeFormatTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testParseTimeWithMilliseconds() {

        long result = TimeFormat.parseTimeWithMilliseconds("20120901T20:10:05.123");

        assertThat(result, is(1346530205123l));
    }

    @Test
    public void testParseTimeWithMillisecondsWithoutMillisecondsInString() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Unable to parse millisecond time string '20120901T20:10:05'");

        long result = TimeFormat.parseTimeWithMilliseconds("20120901T20:10:05");

        assertThat(result, is(0l));
    }

    @Test
    public void testParseTime() {

        long result = TimeFormat.parseTime("20120901T20:10:05");

        assertThat(result, is(1346530205000l));
    }

    @Test
    public void testParseTimeWithAdditinalMillisecondsInString() {

        long result = TimeFormat.parseTime("20120901T20:10:05.123");

        assertThat(result, is(1346530205000l));
    }

    @Test
    public void testParseTimeWithBadString() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Unable to parse time string '20120901T20:10'");

        long result = TimeFormat.parseTime("20120901T20:10");

        assertThat(result, is(0l));
    }
}
