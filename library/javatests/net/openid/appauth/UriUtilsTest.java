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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UriUtilsTest {

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