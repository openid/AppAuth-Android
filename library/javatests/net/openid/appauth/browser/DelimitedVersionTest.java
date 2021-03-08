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

package net.openid.appauth.browser;

import static org.assertj.core.api.Assertions.assertThat;

import net.openid.appauth.BuildConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class DelimitedVersionTest {

    @Test
    public void testParse_emptyString() {
        assertThat(DelimitedVersion.parse("").toString())
                .isEqualTo("0");
    }

    @Test
    public void testParse_nullString() {
        assertThat(DelimitedVersion.parse(null).toString())
                .isEqualTo("0");
    }

    @Test
    public void testParse_singleVersion() {
        assertThat(DelimitedVersion.parse("1").toString())
                .isEqualTo("1");
    }

    @Test
    public void testParse_singleVersionWithPrefix() {
        assertThat(DelimitedVersion.parse("version1").toString())
                .isEqualTo("1");
    }

    @Test
    public void testParse_singleVersionWithSuffix() {
        assertThat(DelimitedVersion.parse("1a").toString())
                .isEqualTo("1");
    }

    @Test
    public void testParse_versionWithTrailingZeros() {
        assertThat(DelimitedVersion.parse("1.0.0").toString())
                .isEqualTo("1");
    }

    @Test
    public void testParse_centerZerosNotIgnored() {
        assertThat(DelimitedVersion.parse("1.0.1.0").toString())
                .isEqualTo("1.0.1");
    }

    @Test
    public void testParse_versionWithDifferentDelimiters() {
        assertThat(DelimitedVersion.parse("1.2-2202").toString())
                .isEqualTo("1.2.2202");
    }

    @Test
    public void testParse_withLongVersionSegment() {
        assertThat(DelimitedVersion.parse("v12.16.04-20161014162559").toString())
                .isEqualTo("12.16.4.20161014162559");
    }

    @Test
    public void testEquals_null() {
        assertThat(DelimitedVersion.parse("1.2")).isNotEqualTo(null);
    }

    @Test
    public void testEquals_differentType() {
        assertThat(DelimitedVersion.parse("1.2")).isNotEqualTo(1);
    }

    @Test
    public void testCompare_withSelf() {
        DelimitedVersion version = DelimitedVersion.parse("1.2");
        assertThat(version.compareTo(version)).isEqualTo(0);
        assertThat(version).isEqualTo(version);
        assertThat(version.hashCode()).isEqualTo(version.hashCode());
    }

    @Test
    public void testCompare_withEquivalent() {
        DelimitedVersion version = DelimitedVersion.parse("1.2");
        DelimitedVersion equivalent = DelimitedVersion.parse("1.2.0.0");
        assertThat(version.compareTo(equivalent)).isEqualTo(0);
        assertThat(version).isEqualTo(equivalent);
        assertThat(version.hashCode()).isEqualTo(equivalent.hashCode());
    }

    @Test
    public void testCompare() {
        DelimitedVersion version = DelimitedVersion.parse("1.2.1");
        DelimitedVersion other = DelimitedVersion.parse("1.3");
        assertThat(version.compareTo(other)).isEqualTo(-1);
        assertThat(other.compareTo(version)).isEqualTo(1);
        assertThat(version).isNotEqualTo(other);
    }

}
