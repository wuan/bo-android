package org.blitzortung.android.jsonrpc;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class JsonRequestEntityTest {

    private JsonRequestEntity jsonRequestEntity;

    @Mock
    private JSONObject jsonObject;

    @Before
    public void setUp() throws UnsupportedEncodingException, JSONException {
        MockitoAnnotations.initMocks(this);

        when(jsonObject.toString()).thenReturn("{}");

        jsonRequestEntity = new JsonRequestEntity(jsonObject);
    }

    @Test
    public void testGetContentType()
    {
        Header header = jsonRequestEntity.getContentType();

        assertThat(header.getName(), is("Content-Type"));
        assertThat(header.getValue(), is("text/json"));
    }


}
