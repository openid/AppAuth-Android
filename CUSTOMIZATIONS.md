# CUSTOMIZATIONS

#### This demo is customized to support any OpenID provider.

#### Details
If clientId is not specified in the /app/java/net/openid/appauthdemo/IdentityProvider.java file.
It will try to register a new app otherwise it will try to authenticate on the basis of client app details.

#### For dynamic registration do the following change in app/res/values/idp_configs_optional.xml file.
    - Change the value of openid_discovery_uri to desired discovery uri of openid

#### For using already created app, do the following changes:
    - Change the value of openid_discovery_uri to desired discovery uri of openid in app/res/values/idp_configs_optional.xml file.
    - Change the value of openid_client_id to desired clientID of desired openid in app/res/values/idp_configs.xml file.
    - Change the value of openid_client_secret to desired clientSecret of desired openid in app/res/values/idp_configs.xml file.
    - Change "NOT_SPECIFIED, // set openid_client_id here" to  R.string.openid_client_id in /app/java/net/openid/appauthdemo/IdentityProvider.java file.
    - Change "NOT_SPECIFIED, // set openid_client_secret here" to  R.string.openid_client_secret /app/java/net/openid/appauthdemo/IdentityProvider.java file.

Run the project.