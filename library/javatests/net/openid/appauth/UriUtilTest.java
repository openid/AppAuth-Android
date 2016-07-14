/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
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

import android.net.UrlQuerySanitizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class UriUtilTest {

    private UrlQuerySanitizer mSanitizer;

    @Before
    public void setUp() {
        mSanitizer = new UrlQuerySanitizer();
        mSanitizer.setAllowUnregisteredParamaters(true);
        mSanitizer.setUnregisteredParameterValueSanitizer(UrlQuerySanitizer.getUrlAndSpaceLegal());
    }

    @Test
    public void testFormUrlEncode() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("test1", "value1");
        parameters.put("test2", "value2");
        String query = UriUtil.formUrlEncode(parameters);

        mSanitizer.parseQuery(query);
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            assertThat(mSanitizer.getValue(param.getKey())).isEqualTo(param.getValue());
        }
    }

    @Test
    public void testFormUrlEncode_withSpaceSeparatedValueForParameter() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("test1", "value1");
        parameters.put("test2", "value2 value3");
        String query = UriUtil.formUrlEncode(parameters);

        assertThat(query.contains("value2+value3"));
        mSanitizer.parseQuery(query);
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            assertThat(mSanitizer.getValue(param.getKey())).isEqualTo(param.getValue());
        }
    }

    @Test
    public void testFormUrlEncode_withNull() {
        assertThat(UriUtil.formUrlEncode(null)).isEqualTo("");
    }

    @Test
    public void testFormUrlEncode_withEmpty() {
        assertThat(UriUtil.formUrlEncode(new HashMap<String, String>())).isEqualTo("");
    }
}
