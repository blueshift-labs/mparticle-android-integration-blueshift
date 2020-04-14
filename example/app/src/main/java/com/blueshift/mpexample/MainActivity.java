package com.blueshift.mpexample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.kits.BlueshiftKit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onLogEventClick(View view) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().logEvent(new MPEvent.Builder("mp_test").build());
        } else {
            Log.e(MainApplication.TAG, "mP instance null");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        BlueshiftKit.registerForInAppMessages(this);
    }

    @Override
    protected void onStop() {
        BlueshiftKit.unregisterForInAppMessages(this);

        super.onStop();
    }
}
