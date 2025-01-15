package com.example.taximeter

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if the user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMainActivity()
        }

        setContentView(R.layout.activity_login)

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpLink = findViewById<TextView>(R.id.signUpLink)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordLink)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val passwordVisibilityIcon = findViewById<ImageView>(R.id.passwordVisibilityIcon)
        val progressBar = findViewById<View>(R.id.progressBar) // Add a ProgressBar to the layout

        var isPasswordVisible = false

        // Toggle password visibility
        passwordVisibilityIcon.setOnClickListener {
            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordVisibilityIcon.setImageResource(R.drawable.invisible) // Set to hide icon
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordVisibilityIcon.setImageResource(R.drawable.visibility) // Set to show icon
            }
            passwordEditText.setSelection(passwordEditText.text.length)
            isPasswordVisible = !isPasswordVisible
        }

        // Login button functionality
        loginButton.setOnClickListener {
            val email = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Basic validation for empty fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show the progress bar while logging in
            progressBar.visibility = View.VISIBLE

            // Sign in with email and password
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    // Hide the progress bar after completion
                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        // Login successful
                        navigateToMainActivity()
                    } else {
                        // Display specific error message
                        val errorMessage = when {
                            task.exception?.message?.contains("password") == true -> "Incorrect password"
                            task.exception?.message?.contains("no user") == true -> "No account found with this email"
                            else -> "Authentication failed. Please try again."
                        }
                        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Navigate to Sign Up Activity when "Sign Up" is clicked
        signUpLink.setOnClickListener {
            navigateToSignUpActivity()
        }

        // Navigate to Forgot Password Activity when "Forgot Password" is clicked
        forgotPasswordLink.setOnClickListener {
            navigateToForgotPasswordActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateToSignUpActivity() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToForgotPasswordActivity() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}
