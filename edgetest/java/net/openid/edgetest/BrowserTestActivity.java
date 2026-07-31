/*
 * Copyright 2024 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.edgetest;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.browser.BrowserDenyList;
import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.BrowserSelector;
import net.openid.appauth.browser.Browsers;
import net.openid.appauth.browser.VersionedBrowserMatcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Test activity for verifying the Edge browser deny list workaround.
 *
 * <p>This app performs three functions:
 * <ol>
 *   <li>Lists all installed browsers with their package names, versions, custom tab support,
 *       and SHA-512 signature hashes (in the format used by AppAuth's BrowserDescriptor).</li>
 *   <li>Tests the BrowserDenyList with Edge exclusion and reports whether Edge is correctly
 *       filtered out of the browser selection.</li>
 *   <li>Extracts the exact Edge signature hash that should be used in Browsers.Edge.SIGNATURE_HASH
 *       for production use.</li>
 * </ol>
 *
 * <p>To use: install this app on a device that has Microsoft Edge installed, then tap each button.
 */
public class BrowserTestActivity extends AppCompatActivity {

    private static final String EDGE_PACKAGE = "com.microsoft.emmx";

    private TextView mOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_test);

        mOutput = findViewById(R.id.output);

        Button btnListBrowsers = findViewById(R.id.btn_list_browsers);
        btnListBrowsers.setOnClickListener(v -> listAllBrowsers());

        Button btnTestDenyList = findViewById(R.id.btn_test_deny_list);
        btnTestDenyList.setOnClickListener(v -> testEdgeDenyList());

        Button btnExtractEdgeHash = findViewById(R.id.btn_extract_edge_hash);
        btnExtractEdgeHash.setOnClickListener(v -> extractEdgeSignatureHash());
    }

    /**
     * Lists all browsers detected by AppAuth's BrowserSelector, showing their package names,
     * versions, custom tab support, and signature hashes.
     */
    private void listAllBrowsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ALL DETECTED BROWSERS ===\n\n");

        List<BrowserDescriptor> browsers = BrowserSelector.getAllBrowsers(this);

        if (browsers.isEmpty()) {
            sb.append("No browsers detected!\n");
        } else {
            sb.append("Found ").append(browsers.size()).append(" browser entries:\n\n");

            for (int i = 0; i < browsers.size(); i++) {
                BrowserDescriptor browser = browsers.get(i);
                sb.append("--- Browser #").append(i + 1).append(" ---\n");
                sb.append("Package: ").append(browser.packageName).append("\n");
                sb.append("Version: ").append(browser.version).append("\n");
                sb.append("Custom Tab: ").append(browser.useCustomTab).append("\n");
                sb.append("Signature Hashes:\n");
                for (String hash : browser.signatureHashes) {
                    sb.append("  ").append(hash).append("\n");
                }

                // Check if this is Edge
                if (EDGE_PACKAGE.equals(browser.packageName)) {
                    sb.append("  >>> THIS IS MICROSOFT EDGE <<<\n");
                }
                sb.append("\n");
            }
        }

        mOutput.setText(sb.toString());
    }

    /**
     * Tests the BrowserDenyList with Edge exclusion.
     * Shows which browser would be selected with and without the deny list.
     */
    private void testEdgeDenyList() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== EDGE DENY LIST TEST ===\n\n");

        // Get all browsers first
        List<BrowserDescriptor> allBrowsers = BrowserSelector.getAllBrowsers(this);

        // Check if Edge is in the list
        boolean edgeFound = false;
        for (BrowserDescriptor browser : allBrowsers) {
            if (EDGE_PACKAGE.equals(browser.packageName)) {
                edgeFound = true;
                break;
            }
        }

        sb.append("Edge installed: ").append(edgeFound ? "YES" : "NO").append("\n\n");

        // Test without deny list (default behavior)
        BrowserDescriptor defaultSelection = BrowserSelector.select(
                this, descriptor -> true);
        sb.append("Default selection (no filter):\n");
        if (defaultSelection != null) {
            sb.append("  Package: ").append(defaultSelection.packageName).append("\n");
            sb.append("  Custom Tab: ").append(defaultSelection.useCustomTab).append("\n");
            sb.append("  Is Edge: ").append(
                    EDGE_PACKAGE.equals(defaultSelection.packageName)).append("\n");
        } else {
            sb.append("  (none)\n");
        }
        sb.append("\n");

        // Test with Edge deny list
        BrowserDenyList denyList = new BrowserDenyList(
                VersionedBrowserMatcher.EDGE_CUSTOM_TAB,
                VersionedBrowserMatcher.EDGE_BROWSER);

        BrowserDescriptor filteredSelection = BrowserSelector.select(this, denyList);
        sb.append("Selection with Edge DenyList:\n");
        if (filteredSelection != null) {
            sb.append("  Package: ").append(filteredSelection.packageName).append("\n");
            sb.append("  Custom Tab: ").append(filteredSelection.useCustomTab).append("\n");
            sb.append("  Is Edge: ").append(
                    EDGE_PACKAGE.equals(filteredSelection.packageName)).append("\n");
        } else {
            sb.append("  (none - no other browser available)\n");
        }
        sb.append("\n");

        // Verify the deny list works correctly
        sb.append("=== VERIFICATION ===\n");
        if (!edgeFound) {
            sb.append("SKIP: Edge is not installed on this device.\n");
            sb.append("Install Microsoft Edge to test the deny list.\n");
        } else if (filteredSelection != null
                && !EDGE_PACKAGE.equals(filteredSelection.packageName)) {
            sb.append("PASS: Edge was successfully excluded!\n");
            sb.append("Alternative browser selected: ")
                    .append(filteredSelection.packageName).append("\n");
        } else if (filteredSelection == null) {
            sb.append("PASS: Edge was excluded (no other browser available).\n");
        } else {
            sb.append("FAIL: Edge was NOT excluded by the deny list.\n");
            sb.append("This likely means the signature hash doesn't match.\n");
            sb.append("Run 'Extract Edge Signature Hash' to get the correct value.\n");
        }

        mOutput.setText(sb.toString());
    }

    /**
     * Extracts the exact SHA-512 signature hash for Microsoft Edge.
     * This is the value that should be used in Browsers.Edge.SIGNATURE_HASH.
     */
    private void extractEdgeSignatureHash() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== EDGE SIGNATURE HASH EXTRACTION ===\n\n");

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    EDGE_PACKAGE, PackageManager.GET_SIGNATURES);

            sb.append("Edge package found!\n");
            sb.append("Package: ").append(packageInfo.packageName).append("\n");
            sb.append("Version: ").append(packageInfo.versionName).append("\n");
            sb.append("Version Code: ").append(packageInfo.versionCode).append("\n\n");

            if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                sb.append("Number of signatures: ")
                        .append(packageInfo.signatures.length).append("\n\n");

                for (int i = 0; i < packageInfo.signatures.length; i++) {
                    Signature sig = packageInfo.signatures[i];
                    String hash = generateSignatureHash(sig);

                    sb.append("Signature #").append(i + 1).append(":\n");
                    sb.append("  SHA-512 (Base64 URL-safe):\n");
                    sb.append("  ").append(hash).append("\n\n");

                    // Check if it matches our current constant
                    sb.append("  Matches Browsers.Edge.SIGNATURE_HASH: ");
                    sb.append(Browsers.Edge.SIGNATURE_HASH.equals(hash) ? "YES" : "NO");
                    sb.append("\n\n");

                    if (!Browsers.Edge.SIGNATURE_HASH.equals(hash)) {
                        sb.append("  *** UPDATE NEEDED ***\n");
                        sb.append("  Replace the SIGNATURE_HASH in Browsers.Edge with:\n");
                        sb.append("  \"").append(hash).append("\"\n\n");
                    }
                }
            } else {
                sb.append("ERROR: No signatures found in package info.\n");
                sb.append("This shouldn't happen for a properly signed app.\n");
            }

        } catch (PackageManager.NameNotFoundException e) {
            sb.append("Microsoft Edge is NOT installed on this device.\n");
            sb.append("Package '").append(EDGE_PACKAGE).append("' not found.\n\n");
            sb.append("Please install Microsoft Edge from the Play Store\n");
            sb.append("and run this test again.\n");
        }

        // Also show the current constant value for reference
        sb.append("\n=== CURRENT CONSTANT VALUE ===\n");
        sb.append("Browsers.Edge.SIGNATURE_HASH:\n");
        sb.append("  ").append(Browsers.Edge.SIGNATURE_HASH).append("\n");

        mOutput.setText(sb.toString());
    }

    /**
     * Generates a SHA-512 hash, Base64 url-safe encoded, from a Signature.
     * This replicates the logic in BrowserDescriptor.generateSignatureHash().
     */
    private static String generateSignatureHash(Signature signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(signature.toByteArray());
            return Base64.encodeToString(hashBytes, Base64.URL_SAFE | Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return "ERROR: SHA-512 not available";
        }
    }
}
