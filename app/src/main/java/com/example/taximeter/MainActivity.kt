package com.example.taximeter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATION_CHANNEL_ID = "ride_notifications"
    private val NOTIFICATION_ID = 1

    private val BASE_FARE = 2.5
    private val FARE_PER_KM = 1.5
    private val FARE_PER_MIN = 0.5
    private var isRunning = false
    private var startTime: Long = 0
    private var totalDistance = 0.0
    private var currentFare = BASE_FARE
    private var lastLocation: Location? = null
    private val traveledPath = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // MapView setup
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Ride control buttons
        findViewById<Button>(R.id.startRideButton).setOnClickListener {
            if (isRunning) stopRide() else startRide()
            updateRideButtonText()
        }
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearMap() }

        // Profile and history navigation
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<ImageButton>(R.id.historyButton).setOnClickListener {
            startActivity(Intent(this, History::class.java))
        }

        // Toggle ride info card visibility
        val toggleButton: ImageButton = findViewById(R.id.toggleRideInfoButton)
        val rideInfoCard: CardView = findViewById(R.id.rideInfoCard)
        toggleButton.setOnClickListener {
            if (rideInfoCard.visibility == View.GONE) {
                rideInfoCard.visibility = View.VISIBLE
                toggleButton.setImageResource(R.drawable.arrowdown)
            } else {
                rideInfoCard.visibility = View.GONE
                toggleButton.setImageResource(R.drawable.arrowup)
            }
        }

        createNotificationChannel()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.setOnMyLocationButtonClickListener {
            moveToCurrentLocation()
            true
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startRide() {
        isRunning = true
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        currentFare = BASE_FARE
        lastLocation = null
        traveledPath.clear()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val startLatLng = LatLng(it.latitude, it.longitude)
                traveledPath.add(startLatLng)
                googleMap.addMarker(MarkerOptions().position(startLatLng).title("Start Point"))
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))
            }
        }
        startLocationUpdates()
    }

    private fun stopRide() {
        Log.d("MainActivity", "stopRide() called") // Debug log

        if (!isRunning) {
            Log.d("MainActivity", "Ride is not running. Exiting stopRide().")
            return
        }

        isRunning = false

        // Stop location updates safely
        if (::locationCallback.isInitialized) {
            try {
                stopLocationUpdates()
                Log.d("MainActivity", "Location updates stopped successfully.")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error stopping location updates: ${e.message}")
            }
        }

        try {
            // Handle the last location and add the stop marker
            if (lastLocation != null) {
                val stopLatLng = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
                googleMap.addMarker(MarkerOptions().position(stopLatLng).title("Stop Point"))
                Log.d("MainActivity", "Stop marker added at: $stopLatLng")

                // Only calculate bounds if there are points in the path
                if (traveledPath.isNotEmpty()) {
                    val bounds = LatLngBounds.Builder().apply {
                        traveledPath.forEach { include(it) }
                    }.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    Log.d("MainActivity", "Camera animated to bounds: $bounds")
                } else {
                    Log.w("MainActivity", "Traveled path is empty. Skipping bounds calculation.")
                }
            } else {
                Log.w("MainActivity", "Last location is null. Stop marker not added.")
                Toast.makeText(this, "Unable to fetch the last location", Toast.LENGTH_SHORT).show()
            }

            // Save the ride details
            saveRideDetails()
            Log.d("MainActivity", "Ride details saved successfully.")

            // Show the notification
            showStopRideNotification()
            Log.d("MainActivity", "Stop ride notification displayed.")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error during stopRide: ${e.message}")
            e.printStackTrace()
        }
    }



    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }



    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { updateFare(it) }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }



    private fun updateFare(location: Location) {
        lastLocation?.let {
            val distance = it.distanceTo(location) / 1000
            totalDistance += distance
            val elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000
            currentFare = BASE_FARE + (totalDistance * FARE_PER_KM) + (elapsedMinutes * FARE_PER_MIN)

            findViewById<TextView>(R.id.distanceTextView).text = String.format("Distance: %.2f km", totalDistance)
            findViewById<TextView>(R.id.timeTextView).text = "Time: $elapsedMinutes min"
            findViewById<TextView>(R.id.fareTextView).text = String.format("Fare: %.2f DH", currentFare)

            traveledPath.add(LatLng(location.latitude, location.longitude))
            googleMap.addPolyline(PolylineOptions().addAll(traveledPath).color(ContextCompat.getColor(this, R.color.teal_200)))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
        }
        lastLocation = location
    }

    private fun clearMap() {
        googleMap.clear()
        totalDistance = 0.0
        currentFare = BASE_FARE
        traveledPath.clear()

        findViewById<TextView>(R.id.distanceTextView).text = "Distance: 0.0 km"
        findViewById<TextView>(R.id.timeTextView).text = "Time: 0 min"
        findViewById<TextView>(R.id.fareTextView).text = "Fare: 0.0 DH"
    }

    private fun saveRideDetails() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val rideData = hashMapOf(
            "distance" to totalDistance,
            "fare" to currentFare,
            "timestamp" to System.currentTimeMillis(),
            "driverId" to userId
        )

        db.collection("drivers").document(userId).collection("rides").add(rideData)
            .addOnSuccessListener {
                Toast.makeText(this, "Ride saved to history", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving ride: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    private fun updateRideButtonText() {
        findViewById<Button>(R.id.startRideButton).text = if (isRunning) "Stop Ride" else "Start Ride"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ride Notifications"
            val descriptionText = "Notifications for ride updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showStopRideNotification() {
        val stopIntent = Intent(this, MainActivity::class.java)
        val stopPendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Ride Completed")
            .setContentText("Your ride has ended. Fare: $currentFare DH")
            .setContentIntent(stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
