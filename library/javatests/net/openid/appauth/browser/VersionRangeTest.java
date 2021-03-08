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
public class VersionRangeTest {

    @Test
    public void testAtLeast() {
        VersionRange range = VersionRange.atLeast("1.2");
        assertThat(range.matches("2")).isTrue();
        assertThat(range.matches("1.5")).isTrue();
        assertThat(range.matches("1.2.1")).isTrue();
        assertThat(range.matches("1.2")).isTrue();
        assertThat(range.matches("1.2.0")).isTrue();

        assertThat(range.matches("1.1.9")).isFalse();
        assertThat(range.matches("1.1")).isFalse();
        assertThat(range.matches("1")).isFalse();
    }

    @Test
    public void testAtMost() {
        VersionRange range = VersionRange.atMost("1.2");
        assertThat(range.matches("2")).isFalse();
        assertThat(range.matches("1.5")).isFalse();
        assertThat(range.matches("1.2.1")).isFalse();
        assertThat(range.matches("1.2")).isTrue();
        assertThat(range.matches("1.2.0")).isTrue();

        assertThat(range.matches("1.1.9")).isTrue();
        assertThat(range.matches("1.1")).isTrue();
        assertThat(range.matches("1")).isTrue();
        assertThat(range.matches("0.5")).isTrue();
    }

    @Test
    public void testBetween() {
        VersionRange range = VersionRange.between("2.0", "2.10");
        assertThat(range.matches("0.8")).isFalse();
        assertThat(range.matches("1.5")).isFalse();
        assertThat(range.matches("1.9")).isFalse();

        assertThat(range.matches("2.0")).isTrue();
        assertThat(range.matches("2.0.1")).isTrue();
        assertThat(range.matches("2.1")).isTrue();
        assertThat(range.matches("2.9.5")).isTrue();
        assertThat(range.matches("2.10")).isTrue();

        assertThat(range.matches("2.10.1")).isFalse();
        assertThat(range.matches("2.11")).isFalse();
        assertThat(range.matches("3.0")).isFalse();
    }

    @Test
    public void testToString_noBounds() {
        assertThat(VersionRange.ANY_VERSION.toString()).isEqualTo("any version");
    }

    @Test
    public void testToString_lowerBoundOnly() {
        assertThat(VersionRange.atLeast("1.2").toString()).isEqualTo("1.2 or higher");
    }

    @Test
    public void testToString_upperBoundOnly() {
        assertThat(VersionRange.atMost("1.2").toString()).isEqualTo("1.2 or lower");
    }

    @Test
    public void testToString_lowerAndUpperBounds() {
        assertThat(VersionRange.between("1.2", "2.0").toString()).isEqualTo("between 1.2 and 2");
    }
}
