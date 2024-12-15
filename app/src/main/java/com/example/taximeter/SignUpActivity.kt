package com.example.taximeter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get references to the EditText fields
        val nomEditText = findViewById<EditText>(R.id.nomEditText) // Nom field
        val prenomEditText = findViewById<EditText>(R.id.prenomEditText) // Prénom field
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val signupPasswordEditText = findViewById<EditText>(R.id.signupPasswordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        signUpButton.setOnClickListener {
            val nom = nomEditText.text.toString() // Get the value of Nom
            val prenom = prenomEditText.text.toString() // Get the value of Prénom
            val email = emailEditText.text.toString()
            val password = signupPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Check if passwords match
            if (password == confirmPassword) {
                // Validate fields
                if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Create user in Firebase Authentication
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Get the current user's ID
                            val userId = auth.currentUser?.uid
                            // Prepare user data to be stored in Firestore
                            val userData = hashMapOf(
                                "nom" to nom,          // Save the Nom
                                "prenom" to prenom,    // Save the Prénom
                                "email" to email
                            )

                            // Save user data to Firestore
                            userId?.let {
                                db.collection("drivers") // The "drivers" collection
                                    .document(it) // Use the user ID as the document ID
                                    .set(userData) // Save the data
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                                        navigateToMainActivity()
                                    }
                                    .addOnFailureListener { e ->
                                        // Detailed error message
                                        Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            // If sign up fails, display a message to the user
                            Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
