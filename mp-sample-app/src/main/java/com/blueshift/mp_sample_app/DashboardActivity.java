package com.blueshift.mp_sample_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.kits.BlueshiftKit;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        boolean isBlueshiftLink = BlueshiftKit.isBlueshiftUniversalLink(getIntent().getData());
        if (isBlueshiftLink)
            BlueshiftKit.handleBlueshiftUniversalLinks(this, getIntent(), null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MParticle.getInstance() != null)
            MParticle.getInstance().logScreen("DashboardActivity");
    }

    public void onLogout(View v) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().Identity().logout()
                    .addSuccessListener(new TaskSuccessListener() {
                        @Override
                        public void onSuccess(@NonNull IdentityApiResult identityApiResult) {
                            launchLoginPage();
                        }
                    })
                    .addFailureListener(new TaskFailureListener() {
                        @Override
                        public void onFailure(@Nullable IdentityHttpResponse identityHttpResponse) {
                            logoutFailedToast();
                        }
                    });
        }
    }

    private void logoutFailedToast() {
        Toast.makeText(this, "Logout failed.", Toast.LENGTH_SHORT).show();
    }

    private void launchLoginPage() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void onLogEventClick(View view) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().logEvent(new MPEvent.Builder("mp_test").build());
        } else {
            Log.e(App.TAG, "mP instance null");
        }
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

        if (MParticle.getInstance() != null) {
            MParticle.getInstance().logEvent(event);
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
