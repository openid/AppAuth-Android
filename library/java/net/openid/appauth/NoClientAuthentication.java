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

/**
 * Implementation of the client authentication method 'none'. This is the default,
 * if no other authentication method is specified when calling
 * {@link AuthorizationService#performTokenRequest(TokenRequest,
 * AuthorizationService.TokenResponseCallback)}.
 *
 * @see @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">
 * "OpenID Connect Core 1.0", Section 9</a>
 */
public class NoClientAuthentication implements ClientAuthentication {
    /**
     * Name of this authentication method.
     */
    public static final String NAME = "none";

    /**
     * The default (singleton) instance of {@linke NoClientAuthentication}.
     */
    public static final NoClientAuthentication INSTANCE = new NoClientAuthentication();

    private NoClientAuthentication() {
        // no need to instantiate separate instances from INSTANCE
    }

    /**
     * {@inheritDoc}
     *
     * @return always null.
     */
    @Override
    public Map<String, String> getRequestHeaders(String clientId) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return always null.
     */
    @Override
    public Map<String, String> getRequestParameters(String clientId) {
        return null;
    }
}
