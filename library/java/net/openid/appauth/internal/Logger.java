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

import static net.openid.appauth.Preconditions.checkNotNull;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Convenience wrapper around {@link android.util.Log}, which evaluates the current log level of
 * the logging tag once and uses this to determine whether logging should proceed. This minimizes
 * the number of native calls made as part of logging.
 */
public final class Logger {

    @VisibleForTesting
    static final String LOG_TAG = "AppAuth";

    @Nullable
    private static Logger sInstance;

    @NonNull
    private final LogWrapper mLog;

    private final int mLogLevel;

    public static synchronized Logger getInstance() {
        if (sInstance == null) {
            sInstance = new Logger(AndroidLogWrapper.INSTANCE);
        }
        return sInstance;
    }

    @VisibleForTesting
    public static synchronized void setInstance(Logger logger) {
        sInstance = logger;
    }

    @VisibleForTesting
    Logger(LogWrapper log) {
        mLog = checkNotNull(log);
        // determine the active logging level
        int level = Log.ASSERT;
        while (level >= Log.VERBOSE && mLog.isLoggable(LOG_TAG, level)) {
            level--;
        }

        mLogLevel = level + 1;
    }

    public static void verbose(String message, Object... messageParams) {
        getInstance().log(Log.VERBOSE, null, message, messageParams);
    }

    public static void verboseWithStack(Throwable tr, String message, Object... messageParams) {
        getInstance().log(Log.VERBOSE, tr, message, messageParams);
    }

    public static void debug(String message, Object... messageParams) {
        getInstance().log(Log.DEBUG, null, message, messageParams);
    }

    public static void debugWithStack(Throwable tr, String message, Object... messageParams) {
        getInstance().log(Log.DEBUG, tr, message, messageParams);
    }

    public static void info(String message, Object... messageParams) {
        getInstance().log(Log.INFO, null, message, messageParams);
    }

    public static void infoWithStack(Throwable tr, String message, Object... messageParams) {
        getInstance().log(Log.INFO, tr, message, messageParams);
    }

    public static void warn(String message, Object... messageParams) {
        getInstance().log(Log.WARN, null, message, messageParams);
    }

    public static void warnWithStack(Throwable tr, String message, Object... messageParams) {
        getInstance().log(Log.WARN, tr, message, messageParams);
    }

    public static void error(String message, Object... messageParams) {
        getInstance().log(Log.ERROR, null, message, messageParams);
    }

    public static void errorWithStack(Throwable tr, String message, Object... messageParams) {
        getInstance().log(Log.ERROR, tr, message, messageParams);
    }

    public void log(int level, Throwable tr, String message, Object... messageParams) {
        if (mLogLevel > level) {
            return;
        }
        String formattedMessage;
        if (messageParams == null || messageParams.length < 1) {
            formattedMessage = message;
        } else {
            formattedMessage = String.format(message, messageParams);
        }

        if (tr != null) {
            formattedMessage += "\n" + mLog.getStackTraceString(tr);
        }

        mLog.println(level, LOG_TAG, formattedMessage);
    }

    /**
     * The core interface of {@link android.util.Log}, converted into instance methods so as to
     * allow easier mock testing.
     */
    @VisibleForTesting
    public interface LogWrapper {
        void println(int level, String tag, String message);

        boolean isLoggable(String tag, int level);

        String getStackTraceString(Throwable tr);
    }

    /**
     * Default {@link LogWrapper} implementation, using {@link android.util.Log} static methods.
     */
    private static final class AndroidLogWrapper implements LogWrapper {
        private static final AndroidLogWrapper INSTANCE = new AndroidLogWrapper();

        private AndroidLogWrapper() {}

        public void println(int level, String tag, String message) {
            Log.println(level, tag, message);
        }

        public boolean isLoggable(String tag, int level) {
            return Log.isLoggable(tag, level);
        }

        public String getStackTraceString(Throwable tr) {
            return Log.getStackTraceString(tr);
        }
    }
}
