package com.blueshift.mpexample;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueshift.model.Configuration;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.kits.BlueshiftKit;

public class MainApplication extends Application implements TaskFailureListener, TaskSuccessListener {
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

        BlueshiftKit.setBlueshiftConfig(configuration);

        IdentityApiRequest identityApiRequest = IdentityApiRequest.withEmptyUser()
                .email("rahulbsft@mp.com")
                .customerId("009")
                .build();

        MParticleOptions options = MParticleOptions.builder(this)
                .credentials("us1-43053bdc5d8a294e89944b82388e2373", "QC9wpXr707rKlvqI88pnghJOx5c8sWRNvEgweg-CSl3c3qQ1SnFxmbcnSDnmeoai")
                .logLevel(MParticle.LogLevel.VERBOSE)
                .identify(identityApiRequest)
                .identifyTask(
                        new BaseIdentityTask()
                                .addFailureListener(this)
                                .addSuccessListener(this)
                )
                .build();

        MParticle.start(options);

        if (MParticle.getInstance() != null) {
            MParticle.getInstance().Messaging().enablePushNotifications("407902546613");
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
