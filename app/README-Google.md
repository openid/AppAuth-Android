# Using AppAuth for Android with Google

1. An OAuth2 client ID for Google Sign In must be created. The
   [quick-start configurator](https://goo.gl/pl2Fu2) can be used to generate this, or it can be
   done directly on the
   [Google Developer Console](https://console.developers.google.com/apis/credentials?project=_).

   With either method of client ID creation, you will need the SHA-1 fingerprint of the certificate
   used to sign the app. After building the app (`./gradlew assembleDebug`), the `appauth.keystore`
   file contains the certificate used and the signature can be displayed by running:

   ```
   keytool -list -v -keystore appauth.keystore -storepass appauth | \
       grep SHA1\: | \
       awk '{print $2}'
   ```

2. The created client ID should look something like `PREFIX.apps.googleusercontent.com`,
   where `PREFIX` is an alphanumeric string unique to your client ID. Replace the `auth_config.json`
   file contents with:

   ```json
   {
     "client_id": "PREFIX.apps.googleusercontent.com",
     "redirect_uri": "com.googleusercontent.apps.PREFIX:/oauth2redirect",
     "authorization_scope": "openid email profile",
     "discovery_uri": "https://accounts.google.com/.well-known/openid-configuration"
   }
   ```

3. Finally, replace the `appAuthRedirectScheme` manifest placeholder in `build.gradle` (for Module: app) with
   `com.googleusercontent.apps.PREFIX`.

After this is done, install the app (`./gradlew :app:installDebug`). Authorizing a Google account
and retrieving user info should now work.
