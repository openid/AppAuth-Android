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

/**
 * AppAuth for Android.
 *
 * <p>AppAuth for Android is a client SDK for communication with
 * <a href="https://tools.ietf.org/html/rfc6749">OAuth2</a> and
 * <a href="http://openid.net/specs/openid-connect-core-1_0.html">OpenID Connect</a> providers. It
 * strives to directly map the requests and responses of those specifications, while following the
 * idiomatic style of the implementation language. In addition to mapping the raw protocol flows,
 * convenience methods are available to assist with common tasks like performing an action with
 * fresh tokens.
 *
 * <p>The library follows the best practices set out in
 * <a href="https://tools.ietf.org/html/rfc8252">RFC 8252 - OAuth 2.0 for Native Apps</a>
 * including using
 * <a href="http://developer.android.com/tools/support-library/features.html#custom-tabs">Custom
 * Tabs</a> for the auth request. For this reason, {@link android.webkit.WebView} is explicitly
 * _not_ supported due to usability and security reasons.
 *
 * <p>The library also supports the <a href="https://tools.ietf.org/html/rfc7636">PKCE</a> extension
 * to OAuth which was created to secure authorization codes in public clients when custom URI scheme
 * redirects are used. The library is friendly to other extensions (standard or otherwise) with the
 * ability to handle additional parameters in all protocol requests and responses.
 */
package net.openid.appauth;
