package net.openid.appauth;

import android.app.PendingIntent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Marina Wageed on 22,September,2020
 * Trufla Technology,
 * Cairo, Egypt.
 */
class PendingLogoutIntentStore
{
    private Queue<LogoutRequest> mRequests = new LinkedList<>();
    private Queue<PendingIntent> mPendingIntents = new LinkedList<>();

    private static PendingLogoutIntentStore sInstance;

    private PendingLogoutIntentStore() {
    }

    public static synchronized PendingLogoutIntentStore getInstance() {
        if (sInstance == null) {
            sInstance = new PendingLogoutIntentStore();
        }
        return sInstance;
    }

    public void addPendingIntent(LogoutRequest request, PendingIntent intent) {
        mRequests.add(request);
        mPendingIntents.add(intent);
    }

    public LogoutRequest getOriginalRequest() {
        return mRequests.poll();
    }

    public PendingIntent getPendingIntent() {
        return mPendingIntents.poll();
    }
}
