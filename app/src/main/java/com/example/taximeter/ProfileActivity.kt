package com.example.taximeter

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var nameEditText: EditText
    private lateinit var prenomEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var licenseEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var qrCodeImageView: ImageView

    private lateinit var homeButton: ImageButton
    private lateinit var historyButton: ImageButton
    private lateinit var profileButton: ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Back button (logout button) click listener
        val logoutButton = findViewById<ImageButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutUser()
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        prenomEditText = findViewById(R.id.prenomEditText)
        emailEditText = findViewById(R.id.emailEditText)
        ageEditText = findViewById(R.id.ageEditText)
        licenseEditText = findViewById(R.id.licenseEditText)
        saveButton = findViewById(R.id.saveButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        homeButton = findViewById(R.id.homeButton)
        historyButton = findViewById(R.id.historyButton)
        profileButton = findViewById(R.id.profileButton)

        // Fetch and display user data
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchDriverData(currentUser.uid)
        }

        // Save button click listener
        saveButton.setOnClickListener {
            saveDriverData()
        }

        // Home Button Click Listener
        homeButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // History Button Click Listener
        historyButton.setOnClickListener {
            startActivity(Intent(this, History::class.java))
        }

        // Profile Button Click Listener
        profileButton.setOnClickListener {
            Toast.makeText(this, "You are already in Profile", Toast.LENGTH_SHORT).show()
        }

        // Language Buttons for selecting language
        val englishButton = findViewById<ImageButton>(R.id.englishButton)
        val frenchButton = findViewById<ImageButton>(R.id.frenchButton)
        val arabicButton = findViewById<ImageButton>(R.id.arabicButton)

        englishButton.setOnClickListener { changeLanguage("en") }
        frenchButton.setOnClickListener { changeLanguage("fr") }
        arabicButton.setOnClickListener { changeLanguage("ar") }
    }

    private fun fetchDriverData(userId: String) {
        db.collection("drivers")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Log the document data to check the content
                    Log.d("Firestore", "Document Data: ${document.data}")

                    // Display predefined fields
                    nameEditText.setText(document.getString("nom") ?: "")
                    prenomEditText.setText(document.getString("prenom") ?: "")
                    emailEditText.setText(document.getString("email") ?: "")
                    ageEditText.setText(document.getString("age") ?: "")
                    licenseEditText.setText(document.getString("licenseType") ?: "")

                    // Generate QR code with user info
                    val nom = document.getString("nom") ?: ""
                    val prenom = document.getString("prenom") ?: ""
                    val email = document.getString("email") ?: ""
                    val age = document.getString("age") ?: ""
                    val licenseType = document.getString("licenseType") ?: ""
                    generateQRCode(nom, prenom, email, age, licenseType)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching user data", e)
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        auth.signOut() // Sign out from Firebase
        val intent = Intent(this, LoginActivity::class.java) // Redirect to login page
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the activity stack
        startActivity(intent)
        finish() // Finish the current activity
    }

    private fun saveDriverData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val updatedData = mutableMapOf(
                "nom" to nameEditText.text.toString(),
                "prenom" to prenomEditText.text.toString(),
                "email" to emailEditText.text.toString(),
                "age" to ageEditText.text.toString(),
                "licenseType" to licenseEditText.text.toString()
            )

            db.collection("drivers")
                .document(userId)
                .set(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    generateQRCode(  // Call generateQRCode to update it after saving the data
                        nameEditText.text.toString(),
                        prenomEditText.text.toString(),
                        emailEditText.text.toString(),
                        ageEditText.text.toString(),
                        licenseEditText.text.toString()
                    )
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateQRCode(nom: String, prenom: String, email: String, age: String, licenseType: String) {
        // Create a string with driver information
        val driverInfo = "Name: $nom $prenom\nEmail: $email\nAge: $age\nLicense Type: $licenseType"

        try {
            // Generate the QR code
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(driverInfo, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500)

            // Display the QR code in ImageView
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // Handle any errors
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        // Update the resources with the new language
        resources.updateConfiguration(config, resources.displayMetrics)

        // Optionally, restart the activity to reflect changes
        Toast.makeText(this, "Language changed to $languageCode", Toast.LENGTH_SHORT).show()
        recreate()  // Recreate the activity to refresh the UI with the new language
    }
}
