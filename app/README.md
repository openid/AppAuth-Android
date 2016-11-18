AppAuth for Android - Demo App
------------------------------

This app demonstrates the AppAuth library by performing an authorization code
flow with Google as the authorization service. While the app compiles without
modification, some configuration is required in order to see Google as an
authorization option

Configuring Google Sign In
==========================

The configuration properties that must be modified can be found in
[res/values/idp_configs.xml](res/values/idp_configs.xml).
First, An OAuth2 client ID for Google Sign In must be created.
The [quick-start configurator](https://goo.gl/pl2Fu2) can be used to
generate this, or it can be done directly on the
[Google Developer Console](https://console.developers.google.com/apis/credentials?project=_).

With either method, you will need the signature of the certificate used to
sign the app. After building the app (`./gradlew assembleDebug`), the
`appauth.keystore` file contains the certificate used and the signature
can be displayed by running:

```
keytool -list -v -keystore appauth.keystore -storepass appauth | \
   grep SHA1\: | \
   awk '{print $2}'
```

The created client ID should look something like:

```
YOUR_CLIENT.apps.googleusercontent.com
```

Where `YOUR_CLIENT` is your client string provided by Google. This full string
should be placed in the `google_client_id` string resource, and the reverse of
it (i.e. `com.googleusercontent.apps.YOUR_CLIENT`) should be placed in
`google_auth_redirect_uri`.
After these values populated, set `google_enabled` to `true`.

Additionally, to enable the capture of redirects to a custom scheme based on
this client ID, modify the `build.gradle` to change the value of the
`appAuthRedirectScheme` manifest placeholder.

After this is done, recompile the app (`./gradlew assembleDebug`) and
install it (`adb install -r -d app/build/outputs/apk/app-debug.apk`). A
Google Sign In button should be displayed.


Adding additional IDPs
======================

Additional authorization services can be added to the demo app by defining
additional instances of `IdentityProvider`. Assuming a service named
`myauth`, the following steps would be taken:

1. The name of the service should be defined in `myauth_name` in
   `idp_configs_optional.xml`.

2. If the service supports OpenID Connect, `myauth_discovery_uri` would be
   defined in `idp_configs_optional.xml` and set to the discovery URI for
   the service
   (e.g. `https://www.myauth.com/.well-known/openid-configuration`).

   Otherwise, `myauth_auth_endpoint_uri` and `myauth_token_endpoint_uri` would
   be defined in `idp_configs_optional.xml` and set to the authorization and
   token endpoint URIs respectively.

4. The default scope string, `myauth_scope_string`, should be defined in
   `idp_configs_optional.xml`.

5. A placeholder for the client ID, `myauth_client_id`, should be defined in
   `idp_configs.xml`.

6. The redirect URI, `myauth_redirect_uri`, can either be defined in
   `idp_configs_optional.xml` or `idp_configs.xml` dependent on whether this
   redirect URI is client ID specific. For instance, if it were just a
   web URL like `https://demo.myauth.com` then it could be placed in
   the optional config file. If it is a custom scheme tied to the client ID,
   similar to what Google defines, it should go in `idp_configs.xml`.

7. An on-off toggle, `myauth_enabled`, should be defined in `idp_configs.xml`
   and set to false by default.

8. Button resources representing the IDP should be imported into the relevant
   directories under `res`.

This may result in an addition to `idp_configs.xml` that looks like:

```
<bool name="myauth_enabled">true</bool>
<string name="myauth_client_id" translatable="false">YOUR_CLIENT_ID</string>
```

And an addition to `idp_configs_optional.xml` that looks like:

```
<string name="myauth_name">MyAuth</string>
<string name="myauth_auth_endpoint_uri">https://www.myauth.com/auth</string>
<string name="myauth_token_endpoint_uri">https://www.myauth.com/token</string>
<string name="myauth_scope_string">profile payment location</string>
<string name="myauth_redirect_uri">https://demo.myauth.com/callback</string>
```

With these properties defined, a new instance of IdentityProvider can be
defined in `IdentityProvider`:

```
public static final MYAUTH = new IdentityProvider(
    "MyAuth", // name of the provider, for debug strings
    R.bool.myauth_enabled,
    NOT_SPECIFIED, // discovery document not provided
    R.string.myauth_auth_endpoint_uri,
    R.string.myauth_token_endpoint_uri,
    R.string.myauth_client_id,
    R.string.myauth_auth_redirect_uri,
    R.string.myauth_scope_string,
    R.drawable.btn_myauth, // your button image asset
    R.string.myauth_name,
    android.R.color.black // text color on the button
);
```

And added to `IdentityProvider`'s static list of IDPs, e.g.:

```
public static final List<IdentityProvider> PROVIDERS = Arrays.asList(MYAUTH)
```

Finally you need to add a new intent-filter to the
`net.openid.appauth.RedirectUriReceiverActivity` activity section of
the `AndroidManifest.xml`

```
<!-- Callback from authentication screen -->
<activity android:name="net.openid.appauth.RedirectUriReceiverActivity">

    <!-- redirect URI for your new IDP -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="@string/your_idp_auth_redirect_scheme"/>
    </intent-filter>
</activity>
```

Make sure you've set `myauth_enabled` to true in the config, and your new IdP
should show up in the list.
