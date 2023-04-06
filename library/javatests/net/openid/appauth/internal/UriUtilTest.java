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

package net.openid.appauth.internal;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import androidx.browser.customtabs.CustomTabsService;

import net.openid.appauth.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
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

    @Test
    public void testToCustomTabUri() {
        Uri exampleUri = Uri.parse("https://www.example.com");
        Uri anotherExampleUri = Uri.parse("https://another.example.com");

        List<Bundle> bundles = UriUtil.toCustomTabUriBundle(
            new Uri[] { exampleUri, anotherExampleUri },
            0);

        assertThat(bundles).hasSize(2);
        assertThat(bundles.get(0).keySet()).contains(CustomTabsService.KEY_URL);
        assertThat(bundles.get(0).get(CustomTabsService.KEY_URL)).isEqualTo(exampleUri);
        assertThat(bundles.get(1).keySet()).contains(CustomTabsService.KEY_URL);
        assertThat(bundles.get(1).get(CustomTabsService.KEY_URL)).isEqualTo(anotherExampleUri);
    }

    @Test
    public void testToCustomTabUri_startIndex() {
        Uri anotherExampleUri = Uri.parse("https://another.example.com");

        List<Bundle> bundles = UriUtil.toCustomTabUriBundle(
            new Uri[] {
                Uri.parse("https://www.example.com"),
                anotherExampleUri
            },
            1);

        assertThat(bundles).hasSize(1);
        assertThat(bundles.get(0).keySet()).contains(CustomTabsService.KEY_URL);
        assertThat(bundles.get(0).get(CustomTabsService.KEY_URL)).isEqualTo(anotherExampleUri);
    }

    @Test
    public void testToCustomTabUriBundle_emptyArray() {
        assertThat(UriUtil.toCustomTabUriBundle(new Uri[0], 0)).isEmpty();
    }

    @Test
    public void testToCustomTabUriBundle_nullArray() {
        assertThat(UriUtil.toCustomTabUriBundle(null, 0)).isEmpty();
    }

    @Test
    public void testToCustomTabUriBundle_startIndexOutsideArray() {
        List<Bundle> bundles = UriUtil.toCustomTabUriBundle(
            new Uri[] {
                Uri.parse("https://www.example.com"),
                Uri.parse("https://another.example.com")
            },
            2);

        assertThat(bundles).hasSize(0);
    }
}
