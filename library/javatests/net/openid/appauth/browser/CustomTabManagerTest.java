package net.openid.appauth.browser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;

import net.openid.appauth.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;


@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class CustomTabManagerTest {

    private static final String BROWSER_PACKAGE_NAME = "com.example.browser";

    @Mock
    Context mContext;

    @Captor
    ArgumentCaptor<Intent> mConnectIntentCaptor;

    @Captor
    ArgumentCaptor<CustomTabsServiceConnection> mConnectionCaptor;

    @Mock
    CustomTabsClient mClient;

    private CustomTabManager mManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mManager = new CustomTabManager(mContext);
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void testBind() {
        startBind(true);
        provideClient();

        // the mock client should now be available on the manager
        assertThat(mManager.getClient()).isEqualTo(mClient);
    }

    @Test
    public void testBind_browserDoesNotSupportCustomTabs() {
        startBind(false);
        assertThat(mManager.getClient()).isEqualTo(null);
    }

    @Test
    public void testCreateSession() {
        startBind(true);
        provideClient();

        CustomTabsCallback mockCallbacks = Mockito.mock(CustomTabsCallback.class);
        CustomTabsSession mockSession = Mockito.mock(CustomTabsSession.class);

        Mockito.doReturn(mockSession).when(mClient).newSession(mockCallbacks);

        Uri launchUri1 = Uri.parse("https://idp.example.com");
        Uri launchUri2 = Uri.parse("https://another.example.com");
        Uri launchUri3 = Uri.parse("https://yetanother.example.com");

        Bundle launchUri2Bundle = new Bundle();
        launchUri2Bundle.putParcelable(CustomTabsService.KEY_URL, launchUri2);

        Bundle launchUri3Bundle = new Bundle();
        launchUri3Bundle.putParcelable(CustomTabsService.KEY_URL, launchUri3);

        CustomTabsSession session = mManager.createSession(
            mockCallbacks,
            launchUri1,
            launchUri2,
            launchUri3);

        assertThat(session).isEqualTo(mockSession);

        // upon creation of the session, the code should prime the session with the expected URIs
        ArgumentCaptor<List> bundleCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockSession).mayLaunchUrl(
            eq(launchUri1),
            eq((Bundle)null),
            bundleCaptor.capture());

        List<Bundle> bundles = bundleCaptor.getValue();
        assertThat(bundles).hasSize(2);
        assertThat(bundles.get(0).get(CustomTabsService.KEY_URL)).isEqualTo(launchUri2);
        assertThat(bundles.get(1).get(CustomTabsService.KEY_URL)).isEqualTo(launchUri3);
    }

    @Test
    public void testCreateSession_browserDoesNotSupportCustomTabs() {
        startBind(false);
        assertThat(mManager.createSession(null)).isNull();
    }

    @Test
    public void testDispose() {
        startBind(true);

    }

    @SuppressWarnings("WrongConstant")
    private void startBind(boolean succeed) {
        Mockito.doReturn(succeed).when(mContext).bindService(
            mConnectIntentCaptor.capture(),
            mConnectionCaptor.capture(),
            Mockito.anyInt());

        mManager.bind(BROWSER_PACKAGE_NAME);

        // check the service connection is made to the specified package
        Intent intent = mConnectIntentCaptor.getValue();
        assertThat(intent.getPackage()).isEqualTo(BROWSER_PACKAGE_NAME);
    }

    private void provideClient() {
        CustomTabsServiceConnection conn = mConnectionCaptor.getValue();
        conn.onCustomTabsServiceConnected(
            new ComponentName(BROWSER_PACKAGE_NAME, BROWSER_PACKAGE_NAME + ".CustomTabsService"),
            mClient);
    }
}
