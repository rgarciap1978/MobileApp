package com.example.mobileapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.android.synthetic.main.activity_home.*
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

enum class ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Setup
        val bundle: Bundle? = intent.extras
        val email: String? = bundle?.getString("email")
        val provider: String? = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")

        // Save data
        val prefs =
            getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()

        // Remote Config
        errorButton.visibility = View.INVISIBLE
        Firebase
            .remoteConfig
            .fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val showErrorButton = Firebase
                        .remoteConfig
                        .getBoolean("show_error_button")

                    val errorButtonText = Firebase
                        .remoteConfig
                        .getString("error_button_text")

                    if (showErrorButton) {
                        errorButton.visibility = View.VISIBLE
                    }

                    errorButton.text = errorButtonText
                }
            }
    }

    private fun setup(email: String, provider: String) {

        title = "Inicio"
        emailTextView.text = email
        providerTextView.text = provider

        logOutButton.setOnClickListener {
            // Delete data
            val prefs =
                getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            if (provider == ProviderType.FACEBOOK.name) {
                LoginManager.getInstance().logOut()
            }

            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        errorButton.setOnClickListener {
            throw RuntimeException("Forzado de Error")
        }

        saveButton.setOnClickListener {

            val map = hashMapOf(
                "provider" to provider,
                "address" to addressTextView.text.toString(),
                "phone" to phoneTextView.text.toString()
            )

            db.collection("users").document(email).set(map)
        }

        getButton.setOnClickListener {
            db.collection("users").document(email)
                .get().addOnSuccessListener {
                    addressTextView.setText(it.get("address") as String?)
                    phoneTextView.setText(it.get("phone") as String?)
                }
        }

        deleteButton.setOnClickListener {
            db.collection("users").document(email).delete()
        }
    }
}
