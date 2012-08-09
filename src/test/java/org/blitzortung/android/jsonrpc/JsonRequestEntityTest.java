package org.blitzortung.android.jsonrpc;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class JsonRequestEntityTest {

    private JsonRequestEntity jsonRequestEntity;

    private JSONObject jsonObject;

    @Before
    public void setUp() throws UnsupportedEncodingException, JSONException {
        //MockitoAnnotations.initMocks(this);

        //when(jsonObject.toString()).thenReturn("{}");
        jsonObject = new JSONObject("{}");

        jsonRequestEntity = new JsonRequestEntity(jsonObject);
    }

    @Ignore
    public void testGetContentType()
    {
        Header header = jsonRequestEntity.getContentType();

        assertThat(header.getName(), is("asd"));
    }


}
