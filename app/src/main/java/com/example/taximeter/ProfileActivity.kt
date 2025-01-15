package com.example.taximeter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
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

    private lateinit var homeButton: ImageButton
    private lateinit var historyButton: ImageButton
    private lateinit var profileButton: ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        initViews()

        // Set up navigation buttons
        setupNavigationButtons()

        // Fetch and display user data
        auth.currentUser?.uid?.let { fetchDriverData(it) }

        // Save button click listener
        saveButton.setOnClickListener {
            saveDriverData()
        }
    }

    private fun initViews() {
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

        // Logout button setup
        val logoutButton = findViewById<ImageButton>(R.id.logoutButton)
        logoutButton.setOnClickListener { logoutUser() }
    }

    private fun setupNavigationButtons() {
        homeButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, History::class.java))
        }

        profileButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.already_in_profile), Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDriverData(userId: String) {
        db.collection("drivers").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nameEditText.setText(document.getString("nom") ?: "")
                    prenomEditText.setText(document.getString("prenom") ?: "")
                    emailEditText.setText(document.getString("email") ?: "")
                    ageEditText.setText(document.getString("age") ?: "")
                    licenseEditText.setText(document.getString("licenseType") ?: "")

                    generateQRCode(
                        nameEditText.text.toString(),
                        prenomEditText.text.toString(),
                        emailEditText.text.toString(),
                        ageEditText.text.toString(),
                        licenseEditText.text.toString()
                    )
                } else {
                    Toast.makeText(this, getString(R.string.user_data_not_found), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error fetching user data", e)
                Toast.makeText(this, getString(R.string.error_fetching_user_data), Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDriverData() {
        val name = nameEditText.text.toString()
        val prenom = prenomEditText.text.toString()
        val age = ageEditText.text.toString().toIntOrNull()
        val licenseType = licenseEditText.text.toString()

        // Validation
        if (name.isEmpty() || prenom.isEmpty() || age == null || licenseType.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (containsNumbers(name)) {
            Toast.makeText(this, "Name cannot contain numbers", Toast.LENGTH_SHORT).show()
            return
        }

        if (containsNumbers(prenom)) {
            Toast.makeText(this, "Prénom cannot contain numbers", Toast.LENGTH_SHORT).show()
            return
        }

        if (age < 18) {
            Toast.makeText(this, "You must be at least 18 years old", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        val updatedData = mapOf(
            "nom" to name,
            "prenom" to prenom,
            "email" to emailEditText.text.toString(),
            "age" to age.toString(),
            "licenseType" to licenseType
        )

        db.collection("drivers").document(userId).set(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.profile_updated_successfully), Toast.LENGTH_SHORT).show()
                generateQRCode(
                    updatedData["nom"] ?: "",
                    updatedData["prenom"] ?: "",
                    updatedData["email"] ?: "",
                    updatedData["age"] ?: "",
                    updatedData["licenseType"] ?: ""
                )
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error saving user data", e)
                Toast.makeText(this, getString(R.string.error_saving_user_data), Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateQRCode(nom: String, prenom: String, email: String, age: String, licenseType: String) {
        val driverInfo = "Name: $nom $prenom\nEmail: $email\nAge: $age\nLicense Type: $licenseType"

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(driverInfo, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500)
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error generating QR code", e)
            Toast.makeText(this, getString(R.string.error_generating_qr_code), Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    // Function to check if the string contains numbers
    private fun containsNumbers(input: String): Boolean {
        return input.any { it.isDigit() }
    }
}
