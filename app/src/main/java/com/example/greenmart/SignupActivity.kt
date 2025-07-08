package com.example.greenmart

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.greenmart.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth and Firebase Realtime Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Signup button click listener
        binding.signupButton.setOnClickListener {
            val signupUsername = binding.signupUsername.text.toString().trim()
            val signupPassword = binding.signupPassword.text.toString().trim()
            val signupEmail = binding.signupEmail.text.toString().trim()
            val signupPhone = binding.signupPhone.text.toString().trim()
            val signupLocation = binding.signupLocation.text.toString().trim()

            // Validate if any fields are empty
            if (signupUsername.isEmpty()) {
                binding.signupUsername.error = "Please enter your username"
                binding.signupUsername.requestFocus()
            } else if (signupEmail.isEmpty()) {
                binding.signupEmail.error = "Please enter your email"
                binding.signupEmail.requestFocus()
            } else if (signupPhone.isEmpty()) {
                binding.signupPhone.error = "Please enter your phone number"
                binding.signupPhone.requestFocus()
            } else if (signupLocation.isEmpty()) {
                binding.signupLocation.error = "Please enter your location"
                binding.signupLocation.requestFocus()
            } else if (signupPassword.isEmpty()) {
                binding.signupPassword.error = "Please enter your password"
                binding.signupPassword.requestFocus()
            } else {
                // Check if email is already registered
                checkIfEmailExists(signupEmail, signupUsername, signupPassword, signupPhone, signupLocation)
            }
        }

        // Redirect to login activity
        binding.loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Social media button click listeners
        binding.facebookBtn.setOnClickListener { openUrl("https://www.facebook.com/r.php") }
        binding.instagramBtn.setOnClickListener { openUrl("https://www.instagram.com/accounts/emailsignup/") }
        binding.twitterBtn.setOnClickListener { openUrl("https://twitter.com/i/flow/signup") }
        binding.tiktokBtn.setOnClickListener { openUrl("https://www.tiktok.com/signup") }
    }

    private fun checkIfEmailExists(email: String, username: String, password: String, phone: String, location: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods != null && signInMethods.isNotEmpty()) {
                        // Email is already registered
                        binding.signupEmail.error = "Email already in use"
                        binding.signupEmail.requestFocus()
                    } else {
                        // Email is not registered, proceed to signup
                        signupWithFirebase(username, password, email, phone, location)
                    }
                } else {
                    Toast.makeText(this, "Error checking email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signupWithFirebase(username: String, password: String, email: String, phone: String, location: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val user = User(username, email, phone, location)
                        database.reference.child("users").child(userId).setValue(user)
                            .addOnCompleteListener { databaseTask ->
                                if (databaseTask.isSuccessful) {
                                    Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to save user data. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    data class User(val username: String, val email: String, val phone: String, val location: String)
}
