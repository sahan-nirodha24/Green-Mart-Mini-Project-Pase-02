package com.example.greenmart

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.greenmart.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth and Firebase Realtime Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Set up the login button click listener
        binding.loginButton.setOnClickListener {
            val loginUsername = binding.loginUsername.text.toString().trim()
            val loginPassword = binding.loginPassword.text.toString().trim()

            // Validate if the fields are empty
            if (loginUsername.isEmpty()) {
                binding.loginUsername.error = "Please enter your username"
                binding.loginUsername.requestFocus()
            } else if (loginPassword.isEmpty()) {
                binding.loginPassword.error = "Please enter your password"
                binding.loginPassword.requestFocus()
            } else {
                // If fields are filled, proceed to check credentials
                loginWithFirebase(loginUsername, loginPassword)
            }
        }

        // Set up the signup redirect button click listener
        binding.signupRedirect.setOnClickListener {
            // Navigate to the SignupActivity
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()  // Finish the current activity to prevent going back to it
        }

        // Redirect to forgot password activity
        binding.ForgotRedirect.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Social media button click listeners
        binding.facebookBtn.setOnClickListener { openUrl("https://www.facebook.com/login/") }
        binding.instagramBtn.setOnClickListener { openUrl("https://www.instagram.com/accounts/login/") }
        binding.twitterBtn.setOnClickListener { openUrl("https://twitter.com/login") }
        binding.tiktokBtn.setOnClickListener { openUrl("https://www.tiktok.com/login/") }
    }

    private fun loginWithFirebase(username: String, password: String) {
        // Firebase Authentication to sign in
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Check if the user exists in the Realtime Database
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        checkUserInDatabase(userId)
                    }
                } else {
                    // Handle login failure
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserInDatabase(userId: String) {
        // Check if user exists in the Realtime Database
        database.reference.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User data exists, login successful
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("USERNAME", snapshot.child("username").value.toString()) // Pass username if needed
                    startActivity(intent)
                    finish()  // Finish the LoginActivity to prevent going back to it
                } else {
                    // User does not exist in the Realtime Database
                    Toast.makeText(this@LoginActivity, "User does not exist in the database", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                Toast.makeText(this@LoginActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
