# Using AppAuth with [FusionAuth](https://fusionauth.io)

[FusionAuth](https://fusionauth.io) is a customer authentication and authorization platform built by developers, for developers. It can be easily integrated to your AppAuth Android app with very few touches:

1. Install FusionAuth on your [Cloud](https://fusionauth.io/docs/v1/tech/installation-guide/cloud), using [Docker](https://fusionauth.io/docs/v1/tech/installation-guide/docker), or check [other options available](https://fusionauth.io/docs/v1/tech/installation-guide/)
2. [Expose the local instance to Internet](https://fusionauth.io/docs/v1/tech/developer-guide/exposing-instance) and copy the address ngrok gave you
3. Log into the admin UI using the ngrok address
4. Browse to `Tenants` and click on the blue pencil icon to edit the **Default** tenant
   * In the `Issuer` field, paste the address you copied earlier
   * Save the tenant
5. Navigate to the `Applications` page, click on the green plus icon to create one
    * Give it a meaningful `Name` (e.g. `My Android App`)
    * Select the `OAuth` tab and fill in these fields:
        * `Client Authentication`: *Not required when using PKCE*
        * `PKCE`: *Required*
        * `Authorized redirect URLs`: your app redirect URI (e.g. `net.openid.appauthdemo:/oauth2redirect`)
    * Go to the `JWT` tab
      * Click on the `Enabled` switch
      * Set both `Access token signing key` and `Id token signing key` to *Auto generate a new key on save...* to generate a new pair of asymmetric keys using the RS256 algorithm
    * Save the application
6. After being redirected back to the `Applications` page, click on the green magnifying glass for the created application to view its details
   * Scroll down to `OAuth2 & OpenID Connect Integration details` and copy the `OpenID Connect Discovery` address
   * In the `OAuth configuration` section, copy the `Client Id` for the newly created application
7. In your Android app, edit `app/res/raw/auth_config.json` and paste the values you copied into lines 2 and 6:
    ```json
    {
      "client_id": "THE CLIENT ID YOU COPIED FROM FUSIONAUTH",
      "redirect_uri": "net.openid.appauthdemo:/oauth2redirect",
      "end_session_redirect_uri": "net.openid.appauthdemo:/oauth2redirect",
      "authorization_scope": "openid offline_access",
      "discovery_uri": "THE OPENID CONNECT DISCOVERY ADDRESS YOU COPIED FROM FUSIONAUTH",
      "https_required": true
    }
    ```
