package com.blueshift.mp_sample_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.MParticleUser;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        emailField = findViewById(R.id.editText);

        if (MParticle.getInstance() != null) {
            MParticleUser cu = MParticle.getInstance().Identity().getCurrentUser();
            if (cu != null && cu.isLoggedIn()) {
                launchDashboard();
            }
        }
    }

    public void onLoginClick(View v) {
        if (MParticle.getInstance() != null) {
            String email = emailField.getText().toString();
            IdentityApiRequest request = IdentityApiRequest.withEmptyUser().email(email).build();
            MParticle.getInstance().Identity().login(request)
                    .addSuccessListener(new TaskSuccessListener() {
                        @Override
                        public void onSuccess(@NonNull IdentityApiResult identityApiResult) {
                            launchDashboard();
                        }
                    })
                    .addFailureListener(new TaskFailureListener() {
                        @Override
                        public void onFailure(@Nullable IdentityHttpResponse identityHttpResponse) {
                            Toast.makeText(
                                    LoginActivity.this, "Login failed.", Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        }
    }

    private void launchDashboard() {
        Intent launcher = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(launcher);
        finish();
    }
}
