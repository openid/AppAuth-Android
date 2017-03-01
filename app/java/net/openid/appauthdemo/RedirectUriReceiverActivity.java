package net.openid.appauthdemo;

import android.app.Activity;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class RedirectUriReceiverActivity extends Activity {
    private static final String TAG = "RedirectUriReceiverActivity";

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        Uri data = getIntent().getData();
        Log.d(TAG, "onCreate(): data=" + data.toString());
        PendingIntent target = PendingIntentStore.getPendingIntentStoreInstance().getPendingIntent();

        if (target == null) {
            Log.e(TAG, "onCreate(): Logout response received but no pending request (intent)");
            finish();
            return;
        }

        Log.d(TAG, "onCreate(): Forwarding redirect");
        try {
            target.send(this, 0, null);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "onCreate(): Unable to send pending intent");
        }

        finish();
    }
}
