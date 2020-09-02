package net.openid.appauth.app2app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.graphics.Color;
import android.net.Uri;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsIntent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import net.openid.appauth.browser.BrowserAllowList;
import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.BrowserSelector;
import net.openid.appauth.browser.VersionedBrowserMatcher;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SecureRedirection {

    private SecureRedirection() {
    }

    /**
     * This method redirects an user securely from one app to another with a given URL. For this to
     * work it is required that the "/.well-known/assetlinks.json" file is correctly set up for this
     * domain and that the target app has an intent-filter for this URL.
     */
    public static void secureRedirection(@NotNull Context context, @NotNull Uri uri) {
        getAssetLinksFile(new RedirectSession(context, uri));
    }

    /**
     * This function retrieves the '/.well-known/assetlinks.json' file from the given domain.
     */
    private static void getAssetLinksFile(@NotNull final RedirectSession redirectSession) {
        RequestQueue volleyQueue = Volley.newRequestQueue(redirectSession.getContext());

        String url =
            redirectSession.getUri().getScheme()
                + "://"
                + redirectSession.getUri().getHost()
                + ":"
                + redirectSession.getUri().getPort()
                + "/.well-known/assetlinks.json";

        JsonArrayRequest request =
            new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        JSONArray baseCertFingerprints =
                            findInstalledApp(redirectSession, response);

                        redirectSession.setBaseCertFingerprints(
                            CertificateFingerprintEncoding
                                .certFingerprintsToDecodedString(
                                    baseCertFingerprints));

                        doRedirection(redirectSession);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.err.println(
                            "Failed to fetch '/.well-known/assetlinks.json' from domain "
                                + "'${redirectSession.uri.host}'\nError: ${error}");
                    }
                });

        volleyQueue.add(request);
    }

    /**
     * Find a suitable installed app to open the URI and return the signing certificate fingerprints
     * for this app. If no such app is found, the signing certificate fingerprints array and the
     * package name will be empty.
     *
     * @param redirectSession
     * @param assetLinks
     * @return
     */
    @NotNull
    private static JSONArray findInstalledApp(
        @NotNull RedirectSession redirectSession, @NotNull JSONArray assetLinks) {
        Pair<Set<String>, Map<String, JSONArray>> basePair =
            getBaseValuesFromAssetLinksFile(assetLinks);
        Set<String> foundPackageNames = getPackageNamesForIntent(redirectSession);

        // Intersect the set of installed apps with the set of apps
        // defined in the '/.well-known/assetlinks.json' file.
        basePair.first.retainAll(foundPackageNames);

        if (basePair.first.iterator().hasNext()) {
            redirectSession.setBasePackageName(basePair.first.iterator().next());
        } else {
            redirectSession.setBasePackageName("");
        }

        JSONArray returnValue = basePair.second.get(redirectSession.getBasePackageName());
        if (returnValue != null) {
            return returnValue;
        }
        return new JSONArray();
    }

    /**
     * Extract the package names and the certificate fingerprints from the
     * '/.well-known/assetlinks.json' file.
     *
     * @param assetLinks
     * @return
     */
    @NotNull
    private static Pair<Set<String>, Map<String, JSONArray>> getBaseValuesFromAssetLinksFile(
        @NotNull JSONArray assetLinks) {
        Set<String> basePackageNames = new HashSet<>();
        Map<String, JSONArray> baseCertFingerprints = new HashMap<>();
        try {
            for (int i = 0; i < assetLinks.length(); i++) {
                JSONObject jsonObject = (JSONObject) assetLinks.get(i);
                JSONObject target = (JSONObject) jsonObject.get("target");
                String basePackageName = target.get("package_name").toString();
                JSONArray baseCertFingerprint = (JSONArray) target.get("sha256_cert_fingerprints");

                basePackageNames.add(basePackageName);
                baseCertFingerprints.put(basePackageName, baseCertFingerprint);
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        return new Pair<>(basePackageNames, baseCertFingerprints);
    }

    /**
     * This method uses the Android Package Manager to find all apps that have an intent-filter for
     * the given URI.
     *
     * @param redirectSession
     * @return
     */
    @NotNull
    private static Set<String> getPackageNamesForIntent(@NotNull RedirectSession redirectSession) {
        /*
           Source: https://stackoverflow.com/questions/11904158/can-i-disable-an-option-when-i-call-intent-action-view
        */
        Intent intent = new Intent(Intent.ACTION_VIEW, redirectSession.getUri());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        List<ResolveInfo> infos =
            redirectSession
                .getContext()
                .getPackageManager()
                .queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

        Set<String> packageNames = new HashSet<>();
        for (ResolveInfo info : infos) {
            packageNames.add(info.activityInfo.packageName);
        }
        return packageNames;
    }

    /**
     * This method checks whether the legit app is installed and either redirect the user to this
     * app or to the default browser.
     */
    private static void doRedirection(@NotNull RedirectSession redirectSession) {
        if (!redirectSession.getBasePackageName().isEmpty() && isAppLegit(redirectSession)) {
            Intent redirectIntent = new Intent(Intent.ACTION_VIEW, redirectSession.getUri());
            redirectIntent.setPackage(redirectSession.getBasePackageName());
            redirectSession.getContext().startActivity(redirectIntent);
        } else {
            redirectToWeb(redirectSession.getContext(), redirectSession.getUri());
        }
    }

    /**
     * This method take a packageName and the signing certificate hash of this package to validate
     * whether the correct app is installed on the device.
     */
    private static boolean isAppLegit(@NotNull RedirectSession redirectSession) {
        Set<String> foundCertFingerprints = getSigningCertificates(redirectSession);
        if (foundCertFingerprints != null) {
            return matchHashes(redirectSession.getBaseCertFingerprints(), foundCertFingerprints);
        }
        return false;
    }

    /**
     * This method retrieves the signing certificate of an app from the Android Package Manager. If
     * the app is not installed this method returns null.
     */
    private static Set<String> getSigningCertificates(@NotNull RedirectSession redirectSession) {
        try {
            Signature[] signatures;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                SigningInfo signingInfo =
                    redirectSession
                        .getContext()
                        .getPackageManager()
                        .getPackageInfo(
                            redirectSession.getBasePackageName(),
                            PackageManager.GET_SIGNING_CERTIFICATES)
                        .signingInfo;
                signatures = signingInfo.getSigningCertificateHistory();
            } else {
                signatures =
                    redirectSession
                        .getContext()
                        .getPackageManager()
                        .getPackageInfo(
                            redirectSession.getBasePackageName(),
                            PackageManager.GET_SIGNATURES)
                        .signatures;
            }
            return BrowserDescriptor.generateSignatureHashes(
                signatures, BrowserDescriptor.DIGEST_SHA_256);
        } catch (PackageManager.NameNotFoundException excepetion) {
            return null;
        }
    }

    /**
     * This function checks whether the two sets contain the same strings independent of their
     * order.
     */
    @VisibleForTesting
    public static boolean matchHashes(
        @NotNull Set<String> certHashes0, @NotNull Set<String> certHashes1) {
        return certHashes0.containsAll(certHashes1) && certHashes0.size() == certHashes1.size();
    }

    /**
     * This method uses the BrowserSelector class to find the user's default browser and validated
     * the integrity of this browser. It then opens the given uri in an Android Custom Tab.
     */
    public static void redirectToWeb(@NotNull Context context, @NotNull Uri uri) {
        redirectToWeb(context, uri, 0, Color.WHITE);
    }

    /**
     * This method uses the BrowserSelector class to find the user's default browser and validated
     * the integrity of this browser. It then opens the given uri in an Android Custom Tab.
     */
    public static void redirectToWeb(
        @NotNull Context context, @NotNull Uri uri, int additionalFlags, int toolbarColor) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(toolbarColor);
        CustomTabsIntent customTabsIntent = builder.build();

        BrowserDescriptor browserDescriptor =
            BrowserSelector.select(
                context,
                new BrowserAllowList(
                    VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                    VersionedBrowserMatcher.CHROME_BROWSER,
                    VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB,
                    VersionedBrowserMatcher.FIREFOX_BROWSER,
                    VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB,
                    VersionedBrowserMatcher.SAMSUNG_BROWSER));

        if (browserDescriptor != null) {
            customTabsIntent
                .intent
                .setPackage(browserDescriptor.packageName)
                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | additionalFlags);
            customTabsIntent.launchUrl(context, uri);
        } else {
            Toast.makeText(context, "Could not find a browser", Toast.LENGTH_SHORT).show();
        }
    }
}
