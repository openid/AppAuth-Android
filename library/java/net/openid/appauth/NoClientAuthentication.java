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

import java.util.Map;

public class NoClientAuthentication implements ClientAuthentication {
    public static final String NAME = "none";

    private static NoClientAuthentication instance = null;

    private NoClientAuthentication() {}

    public static NoClientAuthentication getInstance() {
        if (instance == null) {
            instance = new NoClientAuthentication();
        }
        return instance;
    }

    @Override
    public Map<String, String> getRequestHeaders(String clientId) {
        return null;
    }

    @Override
    public Map<String, String> getRequestParameters(String clientId) {
        return null;
    }
}
