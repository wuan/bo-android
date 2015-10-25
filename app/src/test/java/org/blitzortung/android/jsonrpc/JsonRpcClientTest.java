package org.blitzortung.android.jsonrpc;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class JsonRpcClientTest {

    private JsonRpcClient jsonRpcClient;

    private final String uriString = "foo";

    private final String agentSuffix = "_VERSION";

    @Before
    public void setUp() {
        jsonRpcClient = new JsonRpcClient(uriString, agentSuffix);
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
        Object obj1 = "foo";
        Object obj2 = "bar";
        Object[] parameters = new Object[]{obj1, obj2};

        String methodName = "<methodName>";

        String result = jsonRpcClient.buildRequest(methodName, parameters);

        assertThat(result, is("{\"id\":0,\"method\":\"<methodName>\",\"params\":[\"foo\",\"bar\"]}"));
    }
}
