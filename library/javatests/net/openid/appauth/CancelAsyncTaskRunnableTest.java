/*
 * Copyright 2021 The AppAuth for Android Authors. All Rights Reserved.
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

package net.openid.appauth;

import static android.os.Looper.getMainLooper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.AsyncTask;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPausedAsyncTask;

@SuppressWarnings({"deprecation", "UnstableApiUsage"})
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class CancelAsyncTaskRunnableTest
{
    private PausedExecutorService mPausedExecutorService;

    @Before
    public void setup() {
        mPausedExecutorService = new PausedExecutorService();
        ShadowPausedAsyncTask.overrideExecutor(mPausedExecutorService);
    }

    @Test
    public void testRun() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @SuppressWarnings("StatementWithEmptyBody")
            @Override
            protected Void doInBackground(Void... objects) {
                while (!isCancelled());
                return null;
            }
        };

        CancelAsyncTaskRunnable cancelRunnable = new CancelAsyncTaskRunnable(task);
        assertThat(task.getStatus()).isEqualTo(AsyncTask.Status.PENDING);

        task.execute();
        assertThat(task.getStatus()).isEqualTo(AsyncTask.Status.RUNNING);

        cancelRunnable.run();
        mPausedExecutorService.runAll();
        shadowOf(getMainLooper()).idle();

        assertThat(task.getStatus()).isEqualTo(AsyncTask.Status.FINISHED);
    }
}
