package org.blitzortung.android.jsonrpc;

import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(RobolectricTestRunner.class)
public class JsonRpcClientTest {

    private JsonRpcClient jsonRpcClient;

    private String uriString;

    @Before
    public void setUp() {
        jsonRpcClient = new JsonRpcClient(uriString);
    }

    @Test
    public void testBuildParameters() throws JSONException {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object[] parameters = new Object[]{obj1, obj2};

        JSONArray result = jsonRpcClient.buildParameters(parameters);

        assertThat(result.length(), is(2));

        List<Object> resultObjects = Lists.newArrayList();
        for (int i=0; i<result.length(); i++) {
            resultObjects.add(result.get(i));
        }
        assertThat(resultObjects, hasItems(obj1, obj2));
    }

    @Test
    public void testBuildRequest() throws IOException {
        Object obj1 = new String("foo");
        Object obj2 = new String("bar");
        Object[] parameters = new Object[]{obj1, obj2};

        String methodName = "<methodName>";

        JsonRequestEntity result = jsonRpcClient.buildRequest(methodName, parameters);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(result.getContent()));

        assertThat(bufferedReader.readLine(), is("{\"id\":0,\"method\":\"<methodName>\",\"params\":[\"foo\",\"bar\"]}"));
    }
}
