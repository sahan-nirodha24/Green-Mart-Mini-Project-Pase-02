package com.example.greenmart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var usernameEdit: EditText
    private lateinit var useremailEdit: EditText
    private lateinit var usertelephoneEdit: EditText
    private lateinit var userlocationEdit: EditText
    private lateinit var editButton: Button
    private lateinit var updateButton: Button
    private lateinit var logoutImage: ImageView
    private lateinit var logoutText: TextView

    // Store the original values to check for changes
    private var originalUsername: String = ""
    private var originalEmail: String = ""
    private var originalPhone: String = ""
    private var originalLocation: String = ""

    companion object {
        private const val ARG_USERNAME = "username"

        fun newInstance(username: String?): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putString(ARG_USERNAME, username)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        usernameEdit = view.findViewById(R.id.username_edit)
        useremailEdit = view.findViewById(R.id.useremail_edit)
        usertelephoneEdit = view.findViewById(R.id.usertelephone_edit)
        userlocationEdit = view.findViewById(R.id.userlocation_edit)
        editButton = view.findViewById(R.id.editbtn)
        updateButton = view.findViewById(R.id.updatebtn)
        logoutImage = view.findViewById(R.id.logoutimg)
        logoutText = view.findViewById(R.id.logoutbtn)

        val currentUser = auth.currentUser

        if (currentUser != null) {
            fetchUserDataFromDatabase(currentUser.uid)
        }

        editButton.setOnClickListener {
            setFieldsEnabled(true)
        }

        updateButton.setOnClickListener {
            updateProfile()
        }

        logoutImage.setOnClickListener { logout() }
        logoutText.setOnClickListener { logout() }

        return view
    }

    private fun fetchUserDataFromDatabase(userId: String) {
        database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val userDetails = dataSnapshot.getValue(User::class.java)
                if (userDetails != null) {
                    populateFields(userDetails)
                }
            } else {
                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Failed to fetch user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            Log.e("ProfileFragment", "Error fetching data: ", exception)
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        usernameEdit.isEnabled = enabled
        useremailEdit.isEnabled = enabled
        usertelephoneEdit.isEnabled = enabled
        userlocationEdit.isEnabled = enabled

        editButton.visibility = if (enabled) View.GONE else View.VISIBLE
        updateButton.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun updateProfile() {
        val newUsername = usernameEdit.text.toString().trim()
        val newEmail = useremailEdit.text.toString().trim()
        val newPhone = usertelephoneEdit.text.toString().trim()
        val newLocation = userlocationEdit.text.toString().trim()

        val currentUser = auth.currentUser
        if (currentUser == null) return

        // If no changes were made, exit
        if (newUsername == originalUsername &&
            newEmail == originalEmail &&
            newPhone == originalPhone &&
            newLocation == originalLocation
        ) {
            Toast.makeText(requireContext(), "No changes were made", Toast.LENGTH_SHORT).show()
            setFieldsEnabled(false)
            return
        }

        // Check for duplicate values before updating
        database.child("users").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            var usernameExists = false
            var emailExists = false
            var phoneExists = false

            for (child in snapshot.children) {
                val userId = child.key
                if (userId == currentUser.uid) continue // Skip current user's own record

                val user = child.getValue(User::class.java)
                if (user != null) {
                    if (user.username == newUsername) usernameExists = true
                    if (user.email == newEmail) emailExists = true
                    if (user.phone == newPhone) phoneExists = true
                }
            }

            when {
                phoneExists -> Toast.makeText(requireContext(), "Phone number already exists", Toast.LENGTH_SHORT).show()
                emailExists -> Toast.makeText(requireContext(), "Email already exists", Toast.LENGTH_SHORT).show()
                usernameExists -> Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show()
                else -> {
                    // **Step 1: Delete the previous profile from the database**
                    database.child("users").child(currentUser.uid).removeValue().addOnSuccessListener {

                        // **Step 2: Add the new updated profile**
                        val updatedUser = User(newUsername, newEmail, newPhone, newLocation)

                        database.child("users").child(currentUser.uid).setValue(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                setFieldsEnabled(false)

                                // Update original values
                                originalUsername = newUsername
                                originalEmail = newEmail
                                originalPhone = newPhone
                                originalLocation = newLocation

                                fetchUserDataFromDatabase(currentUser.uid) // Refresh data
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to delete old profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error checking duplicate values", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateFields(userDetails: User) {
        usernameEdit.setText(userDetails.username)
        useremailEdit.setText(userDetails.email)
        usertelephoneEdit.setText(userDetails.phone)
        userlocationEdit.setText(userDetails.location)

        // Store original values for later comparison
        originalUsername = userDetails.username
        originalEmail = userDetails.email
        originalPhone = userDetails.phone
        originalLocation = userDetails.location
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}

data class User(
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = ""
)
