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

package net.openid.appauth.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Log;

import net.openid.appauth.BuildConfig;

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
public final class LoggerTest {

    private static final int INT = 100;

    private AutoCloseable mMockitoCloseable;

    @Mock
    private Logger.LogWrapper mMockLockWrap;

    @Before
    public void setUp() {
        mMockitoCloseable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        Logger.setInstance(null);
        mMockitoCloseable.close();
    }

    @Test
    public void testVerbose_whenVerboseLevel() throws Exception {
        configureLog(Log.VERBOSE);
        Logger.verbose("Test");
        verify(mMockLockWrap).println(Log.VERBOSE, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testVerbose_whenDebugLevel() throws Exception {
        configureLog(Log.DEBUG);
        Logger.verbose("Test");
        verify(mMockLockWrap, never()).println(anyInt(), anyString(), anyString());
    }

    @Test
    public void testDebug_whenVerboseLevel() throws Exception {
        configureLog(Log.VERBOSE);
        Logger.debug("Test");
        verify(mMockLockWrap).println(Log.DEBUG, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testDebug_withMessageParams() throws Exception {
        configureLog(Log.VERBOSE);
        Logger.debug("Test %s %d", "extra", INT);
        verify(mMockLockWrap).println(Log.DEBUG, Logger.LOG_TAG, "Test extra 100");
    }

    @Test
    public void testDebugWithStack() throws Exception {
        configureLog(Log.VERBOSE);
        Logger.debugWithStack(new Exception(), "Bad things happened in %s", "MyClass");
        verify(mMockLockWrap).println(Log.DEBUG, Logger.LOG_TAG,
                "Bad things happened in MyClass\nSTACK");
    }

    @Test
    public void testDebug_whenDebugLevel() throws Exception {
        configureLog(Log.DEBUG);
        Logger.debug("Test");
        verify(mMockLockWrap).println(Log.DEBUG, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testDebug_whenInfoLevel() throws Exception {
        configureLog(Log.INFO);
        Logger.debug("Test");
        verify(mMockLockWrap, never()).println(anyInt(), anyString(), anyString());
    }

    @Test
    public void testInfo_whenDebugLevel() throws Exception {
        configureLog(Log.DEBUG);
        Logger.info("Test");
        verify(mMockLockWrap).println(Log.INFO, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testInfo_whenInfoLevel() throws Exception {
        configureLog(Log.INFO);
        Logger.info("Test");
        verify(mMockLockWrap).println(Log.INFO, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testInfo_whenWarnLevel() throws Exception {
        configureLog(Log.WARN);
        Logger.info("Test");
        verify(mMockLockWrap, never()).println(anyInt(), anyString(), anyString());
    }

    @Test
    public void testWarn_whenInfoLevel() throws Exception {
        configureLog(Log.INFO);
        Logger.warn("Test");
        verify(mMockLockWrap).println(Log.WARN, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testWarn_whenWarnLevel() throws Exception {
        configureLog(Log.WARN);
        Logger.warn("Test");
        verify(mMockLockWrap).println(Log.WARN, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testWarn_whenErrorLevel() throws Exception {
        configureLog(Log.ERROR);
        Logger.warn("Test");
        verify(mMockLockWrap, never()).println(anyInt(), anyString(), anyString());
    }

    @Test
    public void testError_whenWarnLevel() throws Exception {
        configureLog(Log.WARN);
        Logger.error("Test");
        verify(mMockLockWrap).println(Log.ERROR, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testError_whenErrorLevel() throws Exception {
        configureLog(Log.ERROR);
        Logger.error("Test");
        verify(mMockLockWrap).println(Log.ERROR, Logger.LOG_TAG, "Test");
    }

    @Test
    public void testError_whenAssertLevel() throws Exception {
        configureLog(Log.ASSERT);
        Logger.error("Test");
        verify(mMockLockWrap, never()).println(anyInt(), anyString(), anyString());
    }

    private void configureLog(int minLevel) {
        for (int level = Log.VERBOSE; level <= Log.ASSERT; level++) {
            when(mMockLockWrap.isLoggable(Logger.LOG_TAG, level)).thenReturn(level >= minLevel);
        }
        when(mMockLockWrap.getStackTraceString(any(Throwable.class))).thenReturn("STACK");
        Logger.setInstance(new Logger(mMockLockWrap));
    }
}
