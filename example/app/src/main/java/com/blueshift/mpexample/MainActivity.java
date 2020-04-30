package com.blueshift.mpexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.TaskSuccessListener;
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

    public void onLogoutClick(View view) {
        MParticleTask<IdentityApiResult> task = MParticle.getInstance().Identity().logout();
        task.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(@NonNull IdentityApiResult identityApiResult) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    public void onLogEcomEventClick(View view) {
        // 1. Create the products
        Product product = new Product.Builder("Double Room - Econ Rate", "econ-1", 100.00)
                .quantity(4.0)
                .build();

        // 2. Summarize the transaction
        TransactionAttributes attributes = new TransactionAttributes("foo-transaction-id")
                .setRevenue(430.00)
                .setTax(30.00);

        // 3. Log the purchase event
        CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, product)
                .transactionAttributes(attributes)
                .build();

        MParticle.getInstance().logEvent(event);
    }
}
