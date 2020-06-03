package com.blueshift.mp_sample_app;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueshift.model.Configuration;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.kits.BlueshiftKit;

public class App extends Application implements TaskFailureListener, TaskSuccessListener {
    public static final String TAG = "BlueshiftKitSample";

    @Override
    public void onCreate() {
        super.onCreate();
        Configuration configuration = new Configuration();

        // for push
        configuration.setAppIcon(R.drawable.ic_stat_name);

        // in-app
        configuration.setInAppEnabled(true);
        configuration.setJavaScriptForInAppWebViewEnabled(true);
        configuration.setInAppBackgroundFetchEnabled(true);

        BlueshiftKit.setBlueshiftConfig(configuration);

        MParticleOptions options = MParticleOptions.builder(this)
                .credentials(BuildConfig.API_KEY, BuildConfig.API_SECRET)
                .logLevel(MParticle.LogLevel.VERBOSE)
                .identifyTask(
                        new BaseIdentityTask()
                                .addFailureListener(this)
                                .addSuccessListener(this)
                )
                .build();

        MParticle.start(options);

        if (MParticle.getInstance() != null) {
            MParticle.getInstance().Messaging().enablePushNotifications("62519831960");
        }
    }

    @Override
    public void onFailure(@Nullable IdentityHttpResponse identityHttpResponse) {
        Log.e(TAG, "Failure");
    }

    @Override
    public void onSuccess(@NonNull IdentityApiResult identityApiResult) {
        Log.e(TAG, "Success");
    }
}
