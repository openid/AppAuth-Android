/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class JsonUtilTest {
    private static final String TEST_KEY = "key";
    private static final String TEST_STRING = "value";
    private static final Long TEST_LONG = new Long(123);
    private static final String TEST_URI_STRING = "https://openid.net/";
    private static final Uri TEST_URI = Uri.parse(TEST_URI_STRING);
    private static final JSONObject TEST_JSON = new JSONObject();
    private static final JSONArray TEST_ARRAY = new JSONArray();

    static {
        try {
            TEST_JSON.put("a", "b");
        } catch (JSONException e) {
            throw new IllegalStateException("unable to configure test objects");
        }
    }

    private AutoCloseable mMockitoCloseable;

    @Mock
    private JSONObject mJson;

    private JSONObject mRealJson;

    @Before
    public void setUp() {
        mMockitoCloseable = MockitoAnnotations.openMocks(this);
        mRealJson = new JSONObject();
    }

    @After
    public void tearDown() throws Exception {
        mMockitoCloseable.close();
    }

    @Test
    public void testPut() throws Exception {
        JsonUtil.put(mJson, TEST_KEY, TEST_STRING);
        verify(mJson).put(TEST_KEY, TEST_STRING);
    }

    @Test(expected = RuntimeException.class)
    public void testPut_JsonException() throws Exception {
        when(mJson.put(TEST_KEY, TEST_STRING)).thenThrow(new JSONException((String)null));
        JsonUtil.put(mJson, TEST_KEY, TEST_STRING);
    }

    @Test
    public void testPutArray() throws Exception {
        JsonUtil.put(mJson, TEST_KEY, TEST_ARRAY);
        verify(mJson).put(TEST_KEY, TEST_ARRAY);
    }

    @Test(expected = RuntimeException.class)
    public void testPutArray_JsonException() throws Exception {
        when(mJson.put(TEST_KEY, TEST_ARRAY)).thenThrow(new JSONException((String)null));
        JsonUtil.put(mJson, TEST_KEY, TEST_ARRAY);
    }

    @Test
    public void testPutJsonObject() throws Exception {
        JsonUtil.put(mRealJson, TEST_KEY, TEST_JSON);
        assertTrue(mRealJson.has(TEST_KEY));
        assertEquals(TEST_JSON, mRealJson.get(TEST_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testPutJsonObject_nullJson() throws Exception {
        JsonUtil.put(null, TEST_KEY, TEST_JSON);
    }

    @Test(expected = NullPointerException.class)
    public void testPutJsonObject_nullKey() throws Exception {
        JsonUtil.put(mRealJson, null, TEST_JSON);
    }

    @Test
    public void testPutIfNotNullString() throws Exception {
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_STRING);
        verify(mJson).put(TEST_KEY, TEST_STRING);
    }

    @Test
    public void testPutIfNotNullString_null() throws Exception {
        JsonUtil.putIfNotNull(mJson, TEST_KEY, (String) null);
        verify(mJson, never()).put(eq(TEST_KEY), anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testPutIfNotNullString_JsonException() throws Exception {
        when(mJson.put(TEST_KEY, TEST_STRING)).thenThrow(new JSONException((String)null));
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_STRING);
    }

    @Test
    public void testPutIfNotNullUri() throws Exception {
        JsonUtil.putIfNotNull(mRealJson, TEST_KEY, TEST_URI);
        assertTrue(mRealJson.has(TEST_KEY));
        assertEquals(TEST_URI_STRING, mRealJson.getString(TEST_KEY));
    }

    @Test
    public void testPutIfNotNullUri_null() throws Exception {
        JsonUtil.putIfNotNull(mRealJson, TEST_KEY, (Uri) null);
        assertFalse(mRealJson.has(TEST_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testPutIfNotNullUri_nullJson() {
        JsonUtil.putIfNotNull(null, TEST_KEY, TEST_URI);
    }

    @Test(expected = NullPointerException.class)
    public void testPutIfNotNullUri_nullKey() {
        JsonUtil.putIfNotNull(mRealJson, null, TEST_URI);
    }

    @Test(expected = RuntimeException.class)
    public void testPutIfNotNullUri_JsonException() throws Exception {
        when(mJson.put(TEST_KEY, TEST_URI_STRING)).thenThrow(new JSONException((String)null));
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_URI);
    }

    @Test
    public void testPutIfNotNullLong() throws Exception {
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_LONG);
        verify(mJson).put(TEST_KEY, TEST_LONG);
    }

    @Test
    public void testPutIfNotNullLong_null() throws Exception {
        JsonUtil.putIfNotNull(mJson, TEST_KEY, (Long) null);
        verify(mJson, never()).put(eq(TEST_KEY), anyLong());
    }

    @Test(expected = RuntimeException.class)
    public void testPutIfNotNullLong_JsonException() throws Exception {
        when(mJson.put(TEST_KEY, TEST_LONG)).thenThrow(new JSONException((String)null));
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_LONG);
    }

    @Test
    public void testPutIfNotNullJson() throws Exception {
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_JSON);
        verify(mJson).put(TEST_KEY, TEST_JSON);
    }

    @Test
    public void testPutIfNotNullJson_null() throws Exception {
        JsonUtil.putIfNotNull(mJson, TEST_KEY, (JSONObject) null);
        verify(mJson, never()).put(eq(TEST_KEY), any(JSONObject.class));
    }

    @Test(expected = RuntimeException.class)
    public void testPutIfNotNullJson_JsonException() throws Exception {
        when(mJson.put(TEST_KEY, TEST_JSON)).thenThrow(new JSONException((String)null));
        JsonUtil.putIfNotNull(mJson, TEST_KEY, TEST_JSON);
    }

    @Test
    public void testGetString() throws Exception {
        when(mJson.has(TEST_KEY)).thenReturn(true);
        when(mJson.getString(TEST_KEY)).thenReturn(TEST_STRING);
        assertEquals(TEST_STRING, JsonUtil.getString(mJson, TEST_KEY));
    }

    @Test(expected = JSONException.class)
    public void testGetString_missing() throws Exception {
        when(mJson.has(TEST_KEY)).thenReturn(false);
        JsonUtil.getString(mJson, TEST_KEY);
    }

    @Test(expected = JSONException.class)
    public void testGetString_null() throws Exception {
        when(mJson.has(TEST_KEY)).thenReturn(true);
        when(mJson.getString(TEST_KEY)).thenReturn(null);
        JsonUtil.getString(mJson, TEST_KEY);
    }

    @Test(expected = JSONException.class)
    public void testGetStringList_missing() throws Exception {
        when(mJson.has(TEST_KEY)).thenReturn(false);
        JsonUtil.getStringList(mJson, TEST_KEY);
    }

    @Test
    public void testGetStringMap() throws Exception {
        JSONObject mapObj = new JSONObject();
        mapObj.put("a", "1");
        mapObj.put("b", "2");
        mapObj.put("c", "3");

        mRealJson.put(TEST_KEY, mapObj);
        Map<String, String> map = JsonUtil.getStringMap(mRealJson, TEST_KEY);
        assertEquals(mapObj.length(), map.entrySet().size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetStringMap_nullJson() throws Exception {
        JsonUtil.getStringMap(null, TEST_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void testGetStringMap_nullKey() throws Exception {
        JsonUtil.getStringMap(mRealJson, null);
    }
}
