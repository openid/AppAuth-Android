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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class UtilsTest {

    private static final String TEST_STRING = "test_string\nwith a new line";
    private static boolean sIsClosed = false;

    @Test
    public void testCloseQuietly_close() {
        InputStream in = new ByteArrayInputStream(TEST_STRING.getBytes()) {
            @Override
            public void close() throws IOException {
                sIsClosed = true;
                super.close();
            }
        };
        Utils.closeQuietly(in);
        assertTrue(sIsClosed);
    }

    @Test
    public void testCloseQuietly_closed() throws Exception {
        InputStream in = new ByteArrayInputStream(TEST_STRING.getBytes());
        in.close();
        Utils.closeQuietly(in);
    }

    @Test
    public void testCloseQuietly_throw() throws Exception {
        InputStream in = mock(InputStream.class);
        doThrow(new IOException()).when(in).close();
        Utils.closeQuietly(in);
    }

    @Test
    public void testReadInputStream() throws Exception {
        InputStream in = new ByteArrayInputStream(TEST_STRING.getBytes());
        assertEquals(TEST_STRING, Utils.readInputStream(in));
    }

    @Test(expected = IOException.class)
    public void testReadInputStream_throw() throws Exception{
        Utils.readInputStream(null);
    }
}
