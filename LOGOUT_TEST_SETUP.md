# Setup Keycloak

Download and extract Keycloak: http://keycloak.jboss.org/downloads

Start Keycloak:

```
cd $KEY_KLOAK
cd bin
standalone -b 0.0.0.0
```

Open the keycloak administration console: http://localhost:8080

Create the `demo` realm:

 * Move the mouse to `Master` and press `Add realm`
 * Input `demo` as the name of the Realm
 * Press `Save`

Create the "demo-test" client:

 * Press `Clients`
 * Press `Create`
 * Input `demo-test` as the client ID
 * Press `Save`
 * Add `com.test.demo:/oauth2Callback` valid redirect URI for login
 * Add `com.test.logout:/oauth2Callback` valid redirect URI for logout
 * Press `Save`

Add a user:

 * Press `Users`
 * Press `Add user`
 * Input the user name
 * Check `Email Verified`
 * Select the `Credentials` tab
 * Input a password
 * Uncheck `Temporary`
 * Press `Reset Password`

# Resource Files

The mobile phone must have a way to connect to the Keycloak server. The easy way is to configure a hotspot, allowing the mobile phone to directly acccess Keycloak by hostname.

In the `idp_configs_optional.xml` the `KEYCLOAK` string must be replaced to the location of the Keycloak server.