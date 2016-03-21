package net.openid.appauth;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.openid.appauth.AdditionalParamsProcessor.builtInParams;
import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotNull;

public class RegistrationRequest {
    /**
     * OpenID Conenct 'application_type'.
     */
    public static final String APPLICATION_TYPE_NATIVE = "native";

    static final String PARAM_REDIRECT_URIS = "redirect_uris";
    static final String PARAM_RESPONSE_TYPES = "response_types";
    static final String PARAM_GRANT_TYPES = "grant_types";
    static final String PARAM_APPLICATION_TYPE = "application_type";
    static final String PARAM_SUBJECT_TYPE = "subject_type";

    private static final Set<String> BUILT_IN_PARAMS = builtInParams(
            PARAM_REDIRECT_URIS,
            PARAM_RESPONSE_TYPES,
            PARAM_GRANT_TYPES,
            PARAM_APPLICATION_TYPE,
            PARAM_SUBJECT_TYPE
    );
    public static final String SUBJECT_TYPE_PAIRWISE = "pairwise";
    public static final String SUBJECT_TYPE_PUBLIC = "public";


    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri,
     * Uri, Uri) created manually}, or
     * {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)
     * via an OpenID Connect Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * The client's redirect URI.
     */
    @NonNull
    public final List<Uri> redirectUris;

    @NonNull
    public final String applicationType;

    @Nullable
    public final List<String> responseTypes;

    @Nullable
    public final List<String> grantTypes;

    @Nullable
    public final String subjectType;

    /**
     * Additional parameters to be passed as part of the request.
     */
    @NonNull
    public final Map<String, String> additionalParameters;


    /**
     * Creates instances of {@link RegistrationRequest}.
     */
    public static final class Builder {
        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;
        @NonNull
        private List<Uri> mRedirectUris = new ArrayList<>();

        @Nullable
        private List<String> mResponseTypes;

        @Nullable
        private List<String> mGrantTypes;

        @Nullable
        private String mSubjectType;

        @NonNull
        private Map<String, String> mAdditionalParameters = Collections.emptyMap();


        /**
         * Creates a registration request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull Uri redirectUri) {
            setConfiguration(configuration);
            setRedirectUri(redirectUri);
        }

        /**
         * Specifies the authorization service configuration for the request, which must not
         * be null or empty.
         */
        @NonNull
        public Builder setConfiguration(@NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration);
            return this;
        }

        /**
         * Specifies the client's redirect URI. Cannot be null or empty.
         */
        @NonNull
        public Builder setRedirectUri(@NonNull Uri redirectUri) {
            return setRedirectUriValues(checkNotNull(redirectUri, "redirect URI cannot be null or empty"));
        }

        @NonNull
        public Builder setRedirectUriValues(@Nullable Uri... redirectUriValues) {
            return setRedirectUriValues(Arrays.asList(redirectUriValues));
        }

        @NonNull
        public Builder setRedirectUriValues(@Nullable List<Uri> redirectUriValues) {
            mRedirectUris.clear();
            if (redirectUriValues != null) {
                mRedirectUris.addAll(redirectUriValues);
            }
            return this;
        }

        @NonNull
        public Builder setResponseTypeValues(@Nullable String... responseTypeValues) {
            return setResponseTypeValues(Arrays.asList(responseTypeValues));
        }

        @NonNull
        public Builder setResponseTypeValues(@Nullable List<String> responseTypeValues) {
            mResponseTypes = responseTypeValues;
            return this;
        }

        @NonNull
        public Builder setGrantTypeValues(@Nullable String... grantTypeValues) {
            return setGrantTypeValues(Arrays.asList(grantTypeValues));
        }

        @NonNull
        public Builder setGrantTypeValues(@Nullable List<String> grantTypeValues) {
            mGrantTypes = grantTypeValues;
            return this;
        }

        @NonNull
        public Builder setSubjectType(@Nullable String subjectType) {
            mSubjectType = subjectType;
            return this;
        }

        /**
         * Specifies additional parameters. Replaces any previously provided set of parameters.
         * Parameter keys and values cannot be null or empty.
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        @NonNull
        public RegistrationRequest build() {
            return new RegistrationRequest(
                    mConfiguration,
                    Collections.unmodifiableList(mRedirectUris),
                    mResponseTypes == null ? mResponseTypes : Collections.unmodifiableList(mResponseTypes),
                    mGrantTypes == null ? mGrantTypes : Collections.unmodifiableList(mGrantTypes),
                    mSubjectType,
                    Collections.unmodifiableMap(mAdditionalParameters));
        }
    }

    private RegistrationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull List<Uri> redirectUris,
            @Nullable List<String> responseTypes,
            @Nullable List<String> grantTypes,
            @Nullable String subjectType,
            @NonNull Map<String, String> additionalParameters) {
        this.configuration = configuration;
        this.redirectUris = redirectUris;
        this.responseTypes = responseTypes;
        this.grantTypes = grantTypes;
        this.subjectType = subjectType;
        this.additionalParameters = additionalParameters;
        this.applicationType = APPLICATION_TYPE_NATIVE;
    }


    /**
     * Converts the registration request to JSON for transmission.
     */
    @NonNull
    public String toJsonString() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, PARAM_REDIRECT_URIS, JsonUtil.toJsonArray(redirectUris));
        JsonUtil.put(json, PARAM_APPLICATION_TYPE, applicationType);

        if (responseTypes != null) {
            JsonUtil.put(json, PARAM_RESPONSE_TYPES, JsonUtil.toJsonArray(responseTypes));
        }
        if (grantTypes != null) {
            JsonUtil.put(json, PARAM_GRANT_TYPES, JsonUtil.toJsonArray(grantTypes));
        }
        JsonUtil.putIfNotNull(json, PARAM_SUBJECT_TYPE, subjectType);

        for (Map.Entry<String, String> param : additionalParameters.entrySet()) {
            JsonUtil.put(json, param.getKey(), param.getValue());
        }

        return json.toString();
    }
}
