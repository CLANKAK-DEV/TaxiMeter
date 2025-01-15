package com.example.taximeter

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class History : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rideHistoryAdapter: RideHistoryAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rideHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up navigation buttons
        val homeButton: ImageButton = findViewById(R.id.homeButton)
        val historyButton: ImageButton = findViewById(R.id.historyButton)
        val profileButton: ImageButton = findViewById(R.id.profileButton)

        homeButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        historyButton.setOnClickListener {
            Toast.makeText(this, "Already on the History page", Toast.LENGTH_SHORT).show()
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Load the ride history
        loadRideHistory()
    }

    private fun loadRideHistory() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("drivers")
                .document(userId)
                .collection("rides")
                .get()
                .addOnSuccessListener { result ->
                    // Sort the documents by timestamp (latest first) and map them to a list
                    val rideHistory = result.documents
                        .sortedByDescending { it.getLong("timestamp") ?: 0L }
                        .map { document ->
                            val fare = document.getDouble("fare") ?: 0.0
                            val distance = document.getDouble("distance") ?: 0.0
                            val timestamp = document.getLong("timestamp") ?: 0L
                            val formattedTimestamp = formatTimestamp(timestamp)
                            "Fare: $fare DH, Distance: $distance km, Time: $formattedTimestamp"
                        }

                    // Set the data to the RecyclerView adapter
                    rideHistoryAdapter = RideHistoryAdapter(rideHistory)
                    recyclerView.adapter = rideHistoryAdapter
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No user found. Please log in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }
}
