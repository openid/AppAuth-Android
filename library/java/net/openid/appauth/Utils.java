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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class for common operations.
 */
class Utils {
    private static final int INITIAL_READ_BUFFER_SIZE = 1024;

    private Utils() {
        throw new IllegalStateException("This type is not intended to be instantiated");
    }

    /**
     * Read a string from an input stream.
     */
    public static String readInputStream(InputStream in) throws IOException {
        if (in == null) {
            throw new IOException("Input stream must not be null");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        char[] buffer = new char[INITIAL_READ_BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();
        int readCount;
        while ((readCount = br.read(buffer)) != -1) {
            sb.append(buffer, 0, readCount);
        }
        return sb.toString();
    }

    /**
     * Close an input stream quietly, i.e. without throwing an exception.
     */
    public static void closeQuietly(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
            // deliberately do nothing
        }
    }
}
