# Using AppAuth with [Gluu](https://gluu.org/docs/)


The Gluu Server is a free open source identity and access management (IAM) platform. The Gluu Server is a container distribution composed of software written by Gluu and incorporated from other open source projects. Configuration of Gluu server is quick and simple. 

If you do not already have a Gluu Server, you can [read the docs](http://gluu.org/docs/ce) to learn how to download and deploy the software for free.
## Creating OpenID Client on Gluu server

First of all install and login to your gluu server than follow steps below:-

  1. After login, navigate to https://{{Your_gluu_server_domain}}/identity/client/inventory and select **Add Client**
  1. Entre required values to fields( minimum required fileds are :- Client Name,Client Secret,Application Type,Pre-Authorization,Persist Client Authorizations,Logout Session Required) 
  1. Choose **Native** or **web** as the Application Type.
  1. Scroll to bottom and you will find  **"Add Grand type"** button click on it and select **Authorization Code**  as Grant type in opened popup. click ok button in popup to save settings.
  1. Redirect URIs can be like this (**"appscheme://client.example.com"**)
  1. Populate your new OpenID Connect application with values similar to:
  1. Copy the **Client ID**, as it will be needed for the client configuration.
  1. Here you need to set "none" for  "Authentication method for the Token Endpoint" option. otherwise you will be needed to use client secrete in AppAuth for Token refresh which is not recommended to store client secrete in Android app.
        If you still want to use client secrete in you app for  "Authentication method for the Token Endpoint" you can check official doc by [AppAuth](https://github.com/openid/AppAuth-Android/blob/master/README.md#utilizing-client-secrets-dangerous) 
  1. Click **Finish** to redirect back to the *General Settings* of your application.
  
  
**Note:-** You can also create OpenID Clients by using gluu's oxAuth-rp client
  Link for oxAuth-Rp will be https://{{Your_gluu_server_domain}}/oxauth-rp/home.htm 



### Clone the project
https://github.com/openid/AppAuth-Android.git

 clone project from given repo and do following changes in code.

 Finally, within your application update ``res/raw/auth_config.json`` with your settings. You will get a warning if it is incomplete or invalid. Here is an example JSON configuration: 

```json
{
  "client_id": "{{YourClientID}}",
  "redirect_uri": "appscheme://client.example.com",
  "client_secret": "{{Your_client_secret}}",
  "authorization_scope": "openid email profile",
  "discovery_uri": "https://{{your_idp_domain}}/.well-known/openid-configuration",
  "authorization_endpoint_uri": "",
  "token_endpoint_uri": "",
  "registration_endpoint_uri": "",
  "https_required": true
}
```

**Note :-** According to [issue](https://github.com/openid/AppAuth-Android/issues/90) using client secrete in project is not adviced. You must need to a find way keep client secret safe in application.
To pass client secret with token refresh we need to change some minor changes in demo code.

1. **net.openid.appauthdemo.Configuration** :- 
    
    1. create field for client_secret
    ```java
       private String mClientSecret;
    ```
    
    2. In readConfiguration() method add parser for client_secret like this
    ```java
        mClientSecret= getConfigString("client_secret");
    ```   
    
    3. create a getter method for mClientSecret
    ```java
       @Nullable
           public String getClientSecret() {
           return mClientSecret;
       }
    ```


2. **net.openid.appauthdemo.Configuration.TokenActivity**

    1. Change  performTokenRequest with this code.

    ```java
    @MainThread
        private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
            ClientAuthentication clientAuthentication;
            if (Configuration.getInstance(TokenActivity.this).getClientSecret() != null) {
                clientAuthentication = new ClientSecretBasic(Configuration.getInstance(TokenActivity.this).getClientSecret());
    
            } else {
                try {
                    clientAuthentication = mStateManager.getCurrent().getClientAuthentication();
                } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
                    Log.d(TAG, "Token request cannot be made, client authentication for the token "
                        + "endpoint could not be constructed (%s)", ex);
                    displayNotAuthorized("Client authentication method is unsupported");
                    return;
                }
            }
            mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
        }
     ```

#
# Gluu AppAuth Dynamic Registration.

### changes in xml file: 

> If we keep  **client_id** and **client_secret** blank string in  ``res/raw/auth_config.json`` application will automatically register new client to dynamic registration end point.
