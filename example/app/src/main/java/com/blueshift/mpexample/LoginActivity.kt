package com.blueshift.mpexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val currentUser : MParticleUser? = MParticle.getInstance()?.Identity()?.currentUser
        if ( currentUser != null && currentUser.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun onLoginClick(view: View) {
        val apiRequest: IdentityApiRequest = IdentityApiRequest.withEmptyUser().email(editText.text.toString()).build()
        MParticle.getInstance()?.Identity()?.login(apiRequest)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
