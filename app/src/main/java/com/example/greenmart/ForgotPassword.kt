package com.example.greenmart

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.greenmart.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FirebaseAuth and Firebase Realtime Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize ViewBinding
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set onClickListener for the reset password button
        binding.resetPasswordButton.setOnClickListener {
            val username = binding.forgotUsername.text.toString().trim()
            val email = binding.forgotEmail.text.toString().trim()
            val phone = binding.forgotPhone.text.toString().trim()

            var isValid = true

            // Clear previous errors
            binding.forgotUsername.error = null
            binding.forgotEmail.error = null
            binding.forgotPhone.error = null

            // Validate empty fields
            if (username.isEmpty()) {
                binding.forgotUsername.error = "Please enter your username"
                isValid = false
            }
            if (email.isEmpty()) {
                binding.forgotEmail.error = "Please enter your email"
                isValid = false
            }
            if (phone.isEmpty()) {
                binding.forgotPhone.error = "Please enter your phone number"
                isValid = false
            }
            if (!isValid) return@setOnClickListener

            // Check user data in Firebase Realtime Database
            validateUserInDatabase(username, email, phone)
        }

        // Redirect to login activity
        binding.loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Social media button click listeners
        binding.facebookBtn.setOnClickListener { openUrl("https://www.facebook.com/recover/initiate") }
        binding.instagramBtn.setOnClickListener { openUrl("https://www.instagram.com/accounts/password/reset/") }
        binding.twitterBtn.setOnClickListener { openUrl("https://twitter.com/account/begin_password_reset") }
        binding.tiktokBtn.setOnClickListener { openUrl("https://www.tiktok.com/forgot-password") }
    }

    // Validate user in Firebase Realtime Database
    private fun validateUserInDatabase(username: String, email: String, phone: String) {
        database.reference.child("users").orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // If the username exists in the database
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val userEmail = userSnapshot.child("email").getValue(String::class.java)
                        val userPhone = userSnapshot.child("phone").getValue(String::class.java)

                        // Validate email and phone number
                        if (userEmail == email && userPhone == phone) {
                            // User data matches, send password reset email
                            sendPasswordResetEmail(userEmail)
                        } else {
                            // Display errors for email or phone mismatch
                            if (userEmail != email) {
                                binding.forgotEmail.error = "Incorrect email"
                                binding.forgotEmail.requestFocus()
                            }
                            if (userPhone != phone) {
                                binding.forgotPhone.error = "Incorrect phone number"
                                binding.forgotPhone.requestFocus()
                            }
                        }
                    }
                } else {
                    binding.forgotUsername.error = "Username not found"
                    binding.forgotUsername.requestFocus()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ForgotPasswordActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Send password reset email using Firebase Authentication
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent. Check your inbox.", Toast.LENGTH_SHORT).show()
                clearInputFields()
            } else {
                Toast.makeText(this, "Failed to send password reset email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper method to clear input fields
    private fun clearInputFields() {
        binding.forgotUsername.text.clear()
        binding.forgotEmail.text.clear()
        binding.forgotPhone.text.clear()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
