# AppAuth for Android - Demo App

This app demonstrates the AppAuth library by performing an authorization code
flow with an authorization service. The configuration contained in `res/raw/auth_config.json`
must be modified in order for the app to function. Warnings are supplied when the app is run
with an invalid configuration.

The configuration file MUST contain a JSON object. The following properties can be specified:

  - `redirect_uri` (required): The redirect URI to use for receiving the authorization response.
    This can either be a custom scheme URI (com.example.app:/oauth2redirect/example-provider) or 
    an https app link (https://www.example.com/path). Custom scheme URIs are better supported 
    across all versions of Android, however many authorization server implementations require an 
    https URI. Consult the documentation for your authorization server.

    The value specified here should match the value specified for `appAuthRedirectScheme` in the
    `build.gradle` (Module: app), so that the demo app can capture the response.

  - `end_session_redirect_uri` (required): The redirect URI to use for receiving the end session response.
    This should be a custom scheme URI (com.example.app:/oauth2redirect/example-provider). 
    Consult the documentation for your authorization server. 

    The value specified here should match the value specified for `appAuthRedirectScheme` in the
    `build.gradle` (Module: app), so that the demo app can capture the response.
    
    NOTE: Scheme of the URI should be the same as `redirect_uri` but callback should be different.

  - `authorization_scope` (required): The scope string to use for the authorization request.
    For the purposes of the demo, we recommend the value "openid profile email", though any value
    understood by your authorization server can be used.

  - `client_id`: The OAuth2 client id used to identify the client to the authorization server.
    If this property is omitted, or an empty value is provided, dynamic client
    registration will be attempted using the registration URI in the discovery document referenced by
    `discovery_uri` below, or in the value `registration_endpoint_uri`.

  - `discovery_uri`: The OpenID Connect discovery URI for your authorization service, if available.
    If the IDP you wish to test does not support discovery, this value can be omitted or set
    to an empty string.

  - `authorization_endpoint_uri`: The authorization endpoint URI for your authorization service. If
    `discovery_uri` above is not specified, then this value is required. Otherwise, it can be
    omitted or set to an empty string.

  - `token_endpoint_uri`: The token endpoint URI for your authorization service. If `discovery_uri`
    above is not specified, then this value is required. Otherwise, it can be omitted or set to
    an empty string.

  - `registration_endpoint_uri`: The dynamic client registration endpoint URI for your authorization
    service. If client_id and discovery_uri above are not specified, this value MUST be specified.

  - `https_required`: Whether HTTPS connections are required for registration and token requests.
    If omitted, this defaults to true.

## IDP-specific configuration instructions

Each identity provider is free to submit a set of instructions for configuring this demo app to
interact with their authorization endpoints. Those who have submitted instructions are listed
below:

- [Google](README-Google.md)
- [Okta](README-Okta.md)
- [Gluu](README-Gluu.md)

## Should I use this same configuration pattern in my own apps?

We (the AppAuth maintainers) have no strong opinion on this one way or another. Configuration files
can be convenient when the data is likely to change, such as substituting a configuration file
for development versus production builds.

Configurations such as this can also specified in multiple ways; this is just one example approach.
There is also no real harm in hard-coding your configuration into your code, where such dynamic
reconfiguration is unnecessary. Do what is best for your app and development process.
