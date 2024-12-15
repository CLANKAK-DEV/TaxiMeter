package com.example.taximeter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeEncoder

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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Back button click listener
        val backIcon = findViewById<ImageView>(R.id.backIcon)
        backIcon.setOnClickListener {
            onBackPressed()
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

        // Fetch and display user data
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchDriverData(currentUser.uid)
        }

        // Save button click listener
        saveButton.setOnClickListener {
            saveDriverData()
        }
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
                    generateQRCode(nom, prenom, email)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching user data", e)
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
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
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateQRCode(nom: String, prenom: String, email: String) {
        // Create a divider (e.g., a space, comma, or any other symbol)
        val driverInfo = "Name: $nom / $prenom\nEmail: $email" // Divider as '/'
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(driverInfo, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500)
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

}
