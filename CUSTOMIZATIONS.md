# CUSTOMIZATIONS

#### This demo is customized to support any OpenID provider.

#### For dynamic registration do the following change in app/res/values/idp_configs_optional.xml file.

    - Change the value of openid_discovery_uri to desired discovery uri of openid

#### For using already created app, do the following changes:
    - Change the value of openid_discovery_uri to desired discovery uri of openid in app/res/values/idp_configs_optional.xml file.
    - Change the value of openid_client_id to desired clientID of desired openid in app/res/values/idp_configs.xml file.
    - Change "NOT_SPECIFIED, // set openid_client_id here" to  R.string.openid_client_id

Run the project.