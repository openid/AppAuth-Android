![AppAuth for Android](https://rawgit.com/openid/AppAuth-Android/master/appauth_lockup.svg)

[![Download](https://img.shields.io/maven-central/v/net.openid/appauth)](https://search.maven.org/search?q=g:net.openid%20appauth)
[![Javadocs](http://javadoc.io/badge/net.openid/appauth.svg)](http://javadoc.io/doc/net.openid/appauth)
[![Build Status](https://github.com/openid/AppAuth-Android/actions/workflows/build.yml/badge.svg)](https://github.com/openid/AppAuth-Android/actions/workflows/build.yml)
[![codecov.io](https://codecov.io/github/openid/AppAuth-Android/coverage.svg?branch=master)](https://codecov.io/github/openid/AppAuth-Android?branch=master)

AppAuth for Android is a client SDK for communicating with
[OAuth 2.0](https://tools.ietf.org/html/rfc6749) and
[OpenID Connect](http://openid.net/specs/openid-connect-core-1_0.html) providers.
It strives to
directly map the requests and responses of those specifications, while following
the idiomatic style of the implementation language. In addition to mapping the
raw protocol flows, convenience methods are available to assist with common
tasks like performing an action with fresh tokens.

The library follows the best practices set out in
[RFC 8252 - OAuth 2.0 for Native Apps](https://tools.ietf.org/html/rfc8252),
including using
[Custom Tabs](https://developer.chrome.com/multidevice/android/customtabs)
for authorization requests. For this reason,
`WebView` is explicitly *not* supported due to usability and security reasons.

The library also supports the [PKCE](https://tools.ietf.org/html/rfc7636)
extension to OAuth which was created to secure authorization codes in public
clients when custom URI scheme redirects are used. The library is friendly to
other extensions (standard or otherwise) with the ability to handle additional
parameters in all protocol requests and responses.

A talk providing an overview of using the library for enterprise single sign-on (produced by
Google) can be found here:
[Enterprise SSO with Chrome Custom Tabs](https://www.youtube.com/watch?v=DdQTXrk6YTk).

## Download

AppAuth for Android is available on [MavenCentral](https://search.maven.org/search?q=g:net.openid%20appauth)

```groovy
implementation 'net.openid:appauth:<version>'
```

## Requirements

AppAuth supports Android API 16 (Jellybean) and above. Browsers which provide a custom tabs
implementation are preferred by the library, but not required.
Both Custom URI Schemes (all supported versions of Android) and App Links (Android M / API 23+) can
be used with the library.

In general, AppAuth can work with any Authorization Server (AS) that supports
native apps as documented in [RFC 8252](https://tools.ietf.org/html/rfc8252),
either through custom URI scheme redirects, or App Links.
AS's that assume all clients are web-based or require clients to maintain
confidentiality of the client secrets may not work well.

## Demo app

A demo app is contained within this repository. For instructions on how to
build and configure this app, see the
[demo app readme](https://github.com/openid/AppAuth-Android/blob/master/app/README.md).

## Conceptual overview

AppAuth encapsulates the authorization state of the user in the
[net.openid.appauth.AuthState](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthState.java)
class, and communicates with an authorization server through the use of the
[net.openid.appauth.AuthorizationService](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationService.java)
class. AuthState is designed to be easily persistable as a JSON string, using
the storage mechanism of your choice (e.g.
[SharedPreferences](https://developer.android.com/training/basics/data-storage/shared-preferences.html),
[sqlite](https://developer.android.com/training/basics/data-storage/databases.html),
or even just
[in a file](https://developer.android.com/training/basics/data-storage/files.html)).

AppAuth provides data classes which are intended to model the OAuth2
specification as closely as possible; this provides the greatest flexibility
in interacting with a wide variety of OAuth2 and OpenID Connect implementations.

Authorizing the user occurs via the user's web browser, and the request
is described using instances of
[AuthorizationRequest](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationRequest.java).
The request is dispatched using
[performAuthorizationRequest()](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationService.java#L159) on an AuthorizationService instance, and the response (an
[AuthorizationResponse](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationResponse.java) instance) will be dispatched to the activity of your choice,
expressed via an Intent.

Token requests, such as obtaining a new access token using a refresh token,
follow a similar pattern:
[TokenRequest](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/TokenRequest.java) instances are dispatched using
[performTokenRequest()](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationService.java#L252) on an AuthorizationService instance, and a
[TokenResponse](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/TokenResponse.java)
instance is returned via a callback.

Responses can be provided to the
[update()](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthState.java#L367)
methods on AuthState in order to track and persist changes to the authorization
state. Once in an authorized state, the
[performActionWithFreshTokens()](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthState.java#L449)
method on AuthState can be used to automatically refresh access tokens
as necessary before performing actions that require valid tokens.

## Implementing the authorization code flow

It is recommended that native apps use the
[authorization code](https://tools.ietf.org/html/rfc6749#section-1.3.1)
flow with a public client to gain authorization to access user data. This has
the primary advantage for native clients that the authorization flow, which
must occur in a browser, only needs to be performed once.

This flow is effectively composed of four stages:

1. Discovering or specifying the endpoints to interact with the provider.
2. Authorizing the user, via a browser, in order to obtain an authorization
   code.
3. Exchanging the authorization code with the authorization server, to obtain
   a refresh token and/or ID token.
4. Using access tokens derived from the refresh token to interact with a
   resource server for further access to user data.

At each step of the process, an AuthState instance can (optionally) be updated
with the result to help with tracking the state of the flow.

### Authorization service configuration

First, AppAuth must be instructed how to interact with the authorization
service. This can be done either by directly creating an
[AuthorizationServiceConfiguration](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationServiceConfiguration.java#L102)
instance, or by retrieving an OpenID Connect discovery document.

Directly specifying an AuthorizationServiceConfiguration involves
providing the URIs of the authorization endpoint and token endpoint,
and optionally a dynamic client registration endpoint (see "Dynamic client
registration" for more info):

```java
AuthorizationServiceConfiguration serviceConfig =
    new AuthorizationServiceConfiguration(
        Uri.parse("https://idp.example.com/auth"), // authorization endpoint
        Uri.parse("https://idp.example.com/token")); // token endpoint
```

Where available, using an OpenID Connect discovery document is preferable:

```java
AuthorizationServiceConfiguration.fetchFromIssuer(
    Uri.parse("https://idp.example.com"),
    new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
      public void onFetchConfigurationCompleted(
          @Nullable AuthorizationServiceConfiguration serviceConfiguration,
          @Nullable AuthorizationException ex) {
        if (ex != null) {
          Log.e(TAG, "failed to fetch configuration");
          return;
        }

        // use serviceConfiguration as needed
    }
});
```

This will attempt to download a discovery document from the standard location
under this base URI,
`https://idp.example.com/.well-known/openid-configuration`. If the discovery
document for your IDP is in some other non-standard location, you can instead
provide the full URI as follows:

```java
AuthorizationServiceConfiguration.fetchFromUrl(
    Uri.parse("https://idp.example.com/exampletenant/openid-config"),
    new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
        ...
    }
});
```

If desired, this configuration can be used to seed an AuthState instance,
to persist the configuration easily:

```java
AuthState authState = new AuthState(serviceConfig);
```

### Obtaining an authorization code

An authorization code can now be acquired by constructing an
AuthorizationRequest, using its Builder. In AppAuth, the builders for each
data class accept the mandatory parameters via the builder constructor:

```java
AuthorizationRequest.Builder authRequestBuilder =
    new AuthorizationRequest.Builder(
        serviceConfig, // the authorization service configuration
        MY_CLIENT_ID, // the client ID, typically pre-registered and static
        ResponseTypeValues.CODE, // the response_type value: we want a code
        MY_REDIRECT_URI); // the redirect URI to which the auth response is sent
```

Other optional parameters, such as the OAuth2
[scope string](https://tools.ietf.org/html/rfc6749#section-3.3)
or
OpenID Connect
[login hint](http://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1)
are specified through set methods on the builder:

```java
AuthorizationRequest authRequest = authRequestBuilder
    .setScope("openid email profile https://idp.example.com/custom-scope")
    .setLoginHint("jdoe@user.example.com")
    .build();
```

This request can then be dispatched using one of two approaches.

a `startActivityForResult` call using an Intent returned from the
`AuthorizationService`, or by calling `performAuthorizationRequest` and
providing pending intent for completion and cancelation handling activities.

The `startActivityForResult` approach is simpler to use but may require
more processing of the result:

```java
private void doAuthorization() {
  AuthorizationService authService = new AuthorizationService(this);
  Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
  startActivityForResult(authIntent, RC_AUTH);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (requestCode == RC_AUTH) {
    AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
    AuthorizationException ex = AuthorizationException.fromIntent(data);
    // ... process the response or exception ...
  } else {
    // ...
  }
}
```

If instead you wish to directly transition to another activity on completion
or cancelation, you can use `performAuthorizationRequest`:

```java
AuthorizationService authService = new AuthorizationService(this);

authService.performAuthorizationRequest(
    authRequest,
    PendingIntent.getActivity(this, 0, new Intent(this, MyAuthCompleteActivity.class), 0),
    PendingIntent.getActivity(this, 0, new Intent(this, MyAuthCanceledActivity.class), 0));
```

The intents may be customized to carry any additional data or flags required
for the correct handling of the authorization response.

#### Capturing the authorization redirect

Once the authorization flow is completed in the browser, the authorization
service will redirect to a URI specified as part of the authorization request,
providing the response via query parameters. In order for your app to
capture this response, it must register with the Android OS as a handler for
this redirect URI.

We recommend using a custom scheme based redirect URI (i.e. those of form
`my.scheme:/path`), as this is the most widely supported across all versions of
Android. To avoid conflicts with other apps, it is recommended to configure a 
distinct scheme using "reverse domain name notation". This can either match
your service web domain (in reverse) e.g. `com.example.service` or your package
name `com.example.app` or be something completely new as long as it's distinct
enough. Using the package name of your app is quite common but it's not always
possible if it contains illegal characters for URI schemes (like underscores)
or if you already have another handler for that scheme - so just use something
else.

When a custom scheme is used, AppAuth can be easily configured to capture
all redirects using this custom scheme through a manifest placeholder:

```groovy
android.defaultConfig.manifestPlaceholders = [
  'appAuthRedirectScheme': 'com.example.app'
]
```

Alternatively, the redirect URI can be directly configured by adding an
intent-filter for AppAuth's RedirectUriReceiverActivity to your
AndroidManifest.xml:

```xml
<activity
        android:name="net.openid.appauth.RedirectUriReceiverActivity"
        tools:node="replace">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="com.example.app"/>
    </intent-filter>
</activity>
```

If an HTTPS redirect URI is required instead of a custom scheme, the same
approach (modifying your AndroidManifest.xml) is used:

```xml
<activity
        android:name="net.openid.appauth.RedirectUriReceiverActivity"
        tools:node="replace">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="https"
              android:host="app.example.com"
              android:path="/oauth2redirect"/>
    </intent-filter>
</activity>
```

HTTPS redirects can be secured by configuring the redirect URI as an
[app link](https://developer.android.com/training/app-links/index.html) in
Android M and above. We recommend that a fallback page be configured at
the same address to forward authorization responses to your app via a custom
scheme, for older Android devices.

#### Handling the authorization response

Upon completion of the authorization flow, the completion Intent provided to
performAuthorizationRequest will be triggered. The authorization response
is provided to this activity via Intent extra data, which can be extracted
using the `fromIntent()` methods on AuthorizationResponse and
AuthorizationException respectively:

```java
public void onCreate(Bundle b) {
  AuthorizationResponse resp = AuthorizationResponse.fromIntent(getIntent());
  AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
  if (resp != null) {
    // authorization completed
  } else {
    // authorization failed, check ex for more details
  }
  // ...
}
```

The response can be provided to the AuthState instance for easy persistence
and further processing:

```
authState.update(resp, ex);
```

If the full redirect URI is required in order to extract additional information
that AppAuth does not provide, this is also provided to your activity:

```java
public void onCreate(Bundle b) {
  // ...
  Uri redirectUri = getIntent().getData();
  // ...
}
```

### Exchanging the authorization code

Given a successful authorization response carrying an authorization code,
a token request can be made to exchange the code for a refresh token:

```java
authService.performTokenRequest(
    resp.createTokenExchangeRequest(),
    new AuthorizationService.TokenResponseCallback() {
      @Override public void onTokenRequestCompleted(
            TokenResponse resp, AuthorizationException ex) {
          if (resp != null) {
            // exchange succeeded
          } else {
            // authorization failed, check ex for more details
          }
        }
    });
```

The token response can also be used to update an AuthState instance:

```java
authState.update(resp, ex);
```

### Using access tokens

Finally, the retrieved access token can be used to interact with a resource
server. This can be done directly, by extracting the access token from a
token response. However, in most cases, it is simpler to use the
`performActionWithFreshTokens` utility method provided by AuthState:

```java
authState.performActionWithFreshTokens(service, new AuthStateAction() {
  @Override public void execute(
      String accessToken,
      String idToken,
      AuthorizationException ex) {
    if (ex != null) {
      // negotiation for fresh tokens failed, check ex for more details
      return;
    }

    // use the access token to do something ...
  }
});
```

This also updates the AuthState object with current access, id, and refresh tokens.
If you are storing your AuthState in persistent storage, you should write the updated
copy in the callback to this method.

### Ending current session

Given you have a logged in session and you want to end it. In that case you need to get:
- `AuthorizationServiceConfiguration`
- valid Open Id Token that you should get after authentication
- End of session URI that should be provided within you OpenId service config

First you have to build EndSessionRequest

```java
EndSessionRequest endSessionRequest =
    new EndSessionRequest.Builder(authorizationServiceConfiguration)
        .setIdTokenHint(idToken)
        .setPostLogoutRedirectUri(endSessionRedirectUri)
        .build();
```
This request can then be dispatched using one of two approaches.

a `startActivityForResult` call using an Intent returned from the `AuthorizationService`,
or by calling `performEndSessionRequest` and providing pending intent for completion
and cancelation handling activities.

The startActivityForResult approach is simpler to use but may require more processing of the result:

```java
private void endSession() {
  AuthorizationService authService = new AuthorizationService(this);
  Intent endSessionItent = authService.getEndSessionRequestIntent(endSessionRequest);
  startActivityForResult(endSessionItent, RC_END_SESSION);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (requestCode == RC_END_SESSION) {
    EndSessionResonse resp = EndSessionResonse.fromIntent(data);
    AuthorizationException ex = AuthorizationException.fromIntent(data);
    // ... process the response or exception ...
  } else {
    // ...
  }
}
```
If instead you wish to directly transition to another activity on completion or cancelation,
you can use `performEndSessionRequest`:

```java
AuthorizationService authService = new AuthorizationService(this);

authService.performEndSessionRequest(
    endSessionRequest,
    PendingIntent.getActivity(this, 0, new Intent(this, MyAuthCompleteActivity.class), 0),
    PendingIntent.getActivity(this, 0, new Intent(this, MyAuthCanceledActivity.class), 0));
```

End session flow will also work involving browser mechanism that is described in authorization
mechanism session.
Handling response mechanism with transition to another activity should be as follows:

 ```java
public void onCreate(Bundle b) {
  EndSessionResponse resp = EndSessionResponse.fromIntent(getIntent());
  AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
  if (resp != null) {
    // authorization completed
  } else {
    // authorization failed, check ex for more details
  }
  // ...
}
```

### AuthState persistence

Instances of `AuthState` keep track of the authorization and token
requests and responses. This is the only object that you need to persist to
retain the authorization state of the session. Typically, one would do this by
storing the authorization state in SharedPreferences or some other persistent
store private to the app:

```java
@NonNull public AuthState readAuthState() {
  SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
  String stateJson = authPrefs.getString("stateJson", null);
  if (stateJson != null) {
    return AuthState.jsonDeserialize(stateJson);
  } else {
    return new AuthState();
  }
}

public void writeAuthState(@NonNull AuthState state) {
  SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
  authPrefs.edit()
      .putString("stateJson", state.jsonSerializeString())
      .apply();
}
```

The demo app has an [AuthStateManager](https://github.com/openid/AppAuth-Android/blob/master/app/java/net/openid/appauthdemo/AuthStateManager.java)
type which demonstrates this in more detail.

## Advanced configuration

AppAuth provides some advanced configuration options via
[AppAuthConfiguration](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AppAuthConfiguration.java)
instances, which can be provided to
[AuthorizationService](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationService.java)
during construction.

### Controlling which browser is used for authorization

Some applications require explicit control over which browsers can be used
for authorization - for example, to require that Chrome be used for
second factor authentication to work, or require that some custom browser
is used for authentication in an enterprise environment.

Control over which browsers can be used can be achieved by defining a
[BrowserMatcher](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/BrowserMatcher.java), and supplying this to the builder of AppAuthConfiguration.
A BrowserMatcher is suppled with a
[BrowserDescriptor](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/BrowserDescriptor.java)
instance, and must decide whether this browser is permitted for the
authorization flow.

By default, [AnyBrowserMatcher](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/AnyBrowserMatcher.java)
is used.

For your convenience, utility classes to help define a browser matcher are
provided, such as:

- [Browsers](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/Browsers.java):
  contains a set of constants for the official package names and signatures
  of Chrome, Firefox and Samsung SBrowser.
- [VersionedBrowserMatcher](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/VersionedBrowserMatcher.java):
  will match a browser if it has a matching package name and signature, and
  a version number within a defined
  [VersionRange](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/VersionRange.java). This class also provides some static instances for matching
  Chrome, Firefox and Samsung SBrowser.
- [BrowserAllowList](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/BrowserAllowList.java):
  takes a list of BrowserMatcher instances, and will match a browser if any
  of these child BrowserMatcher instances signals a match.
- [BrowserDenyList](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/browser/BrowserDenyList.java):
  the inverse of BrowserAllowList - takes a list of browser matcher instances,
  and will match a browser if it _does not_ match any of these child
  BrowserMatcher instances.

For instance, in order to restrict the authorization flow to using Chrome
or SBrowser as a custom tab:

```java
AppAuthConfiguration appAuthConfig = new AppAuthConfiguration.Builder()
    .setBrowserMatcher(new BrowserAllowList(
        VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
        VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB))
    .build();
AuthorizationService authService =
        new AuthorizationService(context, appAuthConfig);
```

Or, to prevent the use of a buggy version of the custom tabs in
Samsung SBrowser:

```java
AppAuthConfiguration appAuthConfig = new AppAuthConfiguration.Builder()
    .setBrowserMatcher(new BrowserDenyList(
        new VersionedBrowserMatcher(
            Browsers.SBrowser.PACKAGE_NAME,
            Browsers.SBrowser.SIGNATURE_SET,
            true, // when this browser is used via a custom tab
            VersionRange.atMost("5.3")
        )))
    .build();
AuthorizationService authService =
        new AuthorizationService(context, appAuthConfig);
```

### Customizing the connection builder for HTTP requests

It can be desirable to customize how HTTP connections are made when performing
token requests, for instance to use
[certificate pinning](https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning)
or to add additional trusted certificate authorities for an enterprise
environment. This can be achieved in AppAuth by providing a custom
[ConnectionBuilder](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/connectivity/ConnectionBuilder.java)
instance.

For example, to custom the SSL socket factory used, one could do the following:

```java
AppAuthConfiguration appAuthConfig = new AppAuthConfiguration.Builder()
    .setConnectionBuilder(new ConnectionBuilder() {
      public HttpURLConnection openConnect(Uri uri) throws IOException {
        URL url = new URL(uri.toString());
        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsUrlConnection) {
          HttpsURLConnection connection = (HttpsURLConnection) connection;
          connection.setSSLSocketFactory(MySocketFactory.getInstance());
        }
      }
    })
    .build();
```

### Issues with [ID Token](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/IdToken.java#L118) validation

ID Token validation was introduced in `0.8.0` but not all authorization servers or configurations support it correctly.

- For testing environments [setSkipIssuerHttpsCheck](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AppAuthConfiguration.java#L129) can be used to bypass the fact the issuer needs to be HTTPS.

```java
AppAuthConfiguration appAuthConfig = new AppAuthConfiguration.Builder()
    .setSkipIssuerHttpsCheck(true)
    .build()
```

- For services that don't support nonce[s] resulting in **IdTokenException** `Nonce mismatch` just set nonce to `null` on the `AuthorizationRequest`. Please consider **raising an issue** with your Identity Provider and removing this once it is fixed.

```java
AuthorizationRequest authRequest = authRequestBuilder
    .setNonce(null)
    .build();
```

## Dynamic client registration

AppAuth supports the
[OAuth2 dynamic client registration protocol](https://tools.ietf.org/html/rfc7591).
In order to dynamically register a client, create a
[RegistrationRequest](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/RegistrationRequest.java) and dispatch it using
[performRegistrationRequest](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationService.java#L278)
on your AuthorizationService instance.

The registration endpoint can either
be defined directly as part of your
[AuthorizationServiceConfiguration](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/AuthorizationServiceConfiguration.java),
or discovered from an OpenID Connect discovery document.

```java
RegistrationRequest registrationRequest = new RegistrationRequest.Builder(
    serviceConfig,
    Arrays.asList(redirectUri))
    .build();
```

Requests are dispatched with the help of `AuthorizationService`. As this
request is asynchronous the response is passed to a callback:

```java
service.performRegistrationRequest(
    registrationRequest,
    new AuthorizationService.RegistrationResponseCallback() {
        @Override public void onRegistrationRequestCompleted(
            @Nullable RegistrationResponse resp,
            @Nullable AuthorizationException ex) {
            if (resp != null) {
                // registration succeeded, store the registration response
                AuthState state = new AuthState(resp);
                //proceed to authorization...
            } else {
              // registration failed, check ex for more details
            }
         }
    });
```

## Utilizing client secrets (DANGEROUS)

We _strongly recommend_ you avoid using static client secrets in your
native applications whenever possible. Client secrets derived via a dynamic
client registration are safe to use, but static client secrets can be easily
extracted from your apps and allow others to impersonate your app and steal
user data. If client secrets must be used by the OAuth2 provider you are
integrating with, we strongly recommend performing the code exchange step
on your backend, where the client secret can be kept hidden.

Having said this, in some cases using client secrets is unavoidable. In these
cases, a [ClientAuthentication](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/ClientAuthentication.java)
instance can be provided to AppAuth when performing a token request. This
allows additional parameters (both HTTP headers and request body parameters) to
be added to token requests. Two standard implementations of
ClientAuthentication are provided:

- [ClientSecretBasic](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/ClientSecretBasic.java):
  includes a client ID and client secret as an HTTP Basic Authorization header.
- [ClientSecretPost](https://github.com/openid/AppAuth-Android/blob/master/library/java/net/openid/appauth/ClientSecretPost.java):
  includes a client ID and client secret as additional request parameters.

So, in order to send a token request using HTTP basic authorization, one would
write:

```java
ClientAuthentication clientAuth = new ClientSecretBasic(MY_CLIENT_SECRET);
TokenRequest req = ...;
authService.performTokenRequest(req, clientAuth, callback);
```

This can also be done when using `performActionWithFreshTokens` on AuthState:

```java
ClientAuthentication clientAuth = new ClientSecretPost(MY_CLIENT_SECRET);
authState.performActionWithFreshTokens(
    authService,
    clientAuth,
    action);
```

## Modifying or contributing to AppAuth

This project requires the Android SDK for API level 25 (Nougat) to build,
though the produced binaries only require API level 16 (Jellybean) to be
used. We recommend that you fork and/or clone this repository to make
modifications; downloading the source has been known to cause some developers
problems.

For contributors, see the additional instructions in
[CONTRIBUTING.md](https://github.com/openid/AppAuth-Android/blob/master/CONTRIBUTING.md).

### Building from the Command line

AppAuth for Android uses Gradle as its build system. In order to build
the library and app binaries, run `./gradlew assemble`.
The library AAR files are output to `library/build/outputs/aar`, while the
demo app is output to `app/build/outputs/apk`.
In order to run the tests and code analysis, run `./gradlew check`.

### Building from Android Studio

In AndroidStudio, File -> New -> Import project. Select the root folder
(the one with the `build.gradle` file).
