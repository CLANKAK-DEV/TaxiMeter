package com.example.taximeter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Add references for the password visibility icons
    private lateinit var passwordVisibilityIcon: ImageView
    private lateinit var confirmPasswordVisibilityIcon: ImageView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get references to the EditText fields and SignUp Button
        val nomEditText = findViewById<EditText>(R.id.nomEditText)
        val prenomEditText = findViewById<EditText>(R.id.prenomEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val signupPasswordEditText = findViewById<EditText>(R.id.signupPasswordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        // Get references to the password visibility icons
        passwordVisibilityIcon = findViewById(R.id.passwordVisibilityIcon)
        confirmPasswordVisibilityIcon = findViewById(R.id.confirmPasswordVisibilityIcon)

        // Toggle password visibility on icon click
        passwordVisibilityIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            updatePasswordVisibility(signupPasswordEditText)
        }

        confirmPasswordVisibilityIcon.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            updatePasswordVisibility(confirmPasswordEditText)
        }

        signUpButton.setOnClickListener {
            val nom = nomEditText.text.toString()
            val prenom = prenomEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = signupPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Check if fields are empty
            if (nom.isEmpty()) {
                Toast.makeText(this, "Please enter your last name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (prenom.isEmpty()) {
                Toast.makeText(this, "Please enter your first name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate that Nom and Prénom do not contain numbers
            if (containsNumbers(nom)) {
                Toast.makeText(this, "Last name cannot contain numbers.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (containsNumbers(prenom)) {
                Toast.makeText(this, "First name cannot contain numbers.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val userData = hashMapOf(
                            "nom" to nom,
                            "prenom" to prenom,
                            "email" to email
                        )

                        userId?.let {
                            db.collection("drivers")
                                .document(it)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                                    navigateToMainActivity()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // Check if the input contains numbers
    private fun containsNumbers(input: String): Boolean {
        return input.any { it.isDigit() }
    }

    // Toggle password visibility
    private fun updatePasswordVisibility(passwordEditText: EditText) {
        if (passwordEditText == findViewById(R.id.signupPasswordEditText)) {
            // Toggle the visibility for the main password
            if (isPasswordVisible) {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordVisibilityIcon.setImageResource(R.drawable.invisible)
            } else {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordVisibilityIcon.setImageResource(R.drawable.visibility)
            }
        } else {
            // Toggle the visibility for the confirm password
            if (isConfirmPasswordVisible) {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                confirmPasswordVisibilityIcon.setImageResource(R.drawable.invisible)
            } else {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                confirmPasswordVisibilityIcon.setImageResource(R.drawable.visibility)
            }
        }
        passwordEditText.setSelection(passwordEditText.text.length)  // Maintain cursor position
    }

    // Navigate back to the login screen when the 'Already have an account?' TextView is clicked
    fun returnToLogin(view: View) {
        val intent = Intent(this, LoginActivity::class.java)  // Make sure LoginActivity is your login screen
        startActivity(intent)
        finish()  // Optionally close this activity to prevent going back to it
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
