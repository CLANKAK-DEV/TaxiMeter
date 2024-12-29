package com.example.taximeter

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import pub.devrel.easypermissions.EasyPermissions
import java.util.Locale

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
        findViewById<ImageButton>(R.id.historyButton).setOnClickListener {
            // Navigate to the History activity
            val intent = Intent(this, History::class.java)
            startActivity(intent)
        }

        // Initialize MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        findViewById<Button>(R.id.startRideButton).setText(R.string.start_ride)
        findViewById<Button>(R.id.startRideButton).setOnClickListener {
            if (isRunning) stopRide() else startRide()
            updateRideButtonText()
        }

        // Logout Button
        findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            // You can redirect to the main screen here if needed
            startActivity(Intent(this, MainActivity::class.java)) // Replace with your home activity
        }

        // Profile Button
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener { navigateToProfile() }

        // Clear Map Button
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearMap() }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Fetch ride history

        createNotificationChannel()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "We need access to your location to track the ride.",
                LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun startRide() {
        isRunning = true
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        currentFare = BASE_FARE
        lastLocation = null
        traveledPath.clear()

        startLocationUpdates()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val startLatLng = LatLng(it.latitude, it.longitude)
                    traveledPath.add(startLatLng)
                    googleMap.addMarker(
                        MarkerOptions().position(startLatLng).title("Start Location")
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))
                }
            }
        }

        findViewById<TextView>(R.id.timeTextView).text = "Time: 0 min"
    }

    private fun stopRide() {
        isRunning = false
        stopLocationUpdates()

        lastLocation?.let {
            val stopLatLng = LatLng(it.latitude, it.longitude)
            googleMap.addMarker(MarkerOptions().position(stopLatLng).title("Stop Location"))
        }

        saveRideDetails {

        }
        showStopRideNotification()

        totalDistance = 0.0
        currentFare = BASE_FARE
        startTime = 0
        lastLocation = null
        traveledPath.clear()
    }

    private fun updateRideButtonText() {
        val button = findViewById<Button>(R.id.startRideButton)
        button.text = if (isRunning) "Stop Ride" else "Start Ride"
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000  // Fetch updates every 5 seconds
            fastestInterval = 3000  // Allow updates every 3 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { updateFare(it) }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateFare(location: Location) {
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location) / 1000
            totalDistance += distance
            val elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000
            currentFare =
                BASE_FARE + (totalDistance * FARE_PER_KM) + (elapsedMinutes * FARE_PER_MIN)

            findViewById<TextView>(R.id.distanceTextView).text = String.format("Distance: %.3f km", totalDistance)
            findViewById<TextView>(R.id.timeTextView).text = "Time: $elapsedMinutes min"
            findViewById<TextView>(R.id.fareTextView).text = String.format("Fare: %.3f DH", currentFare)

            val currentLatLng = LatLng(location.latitude, location.longitude)
            traveledPath.add(currentLatLng)
            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(traveledPath)
                    .color(ContextCompat.getColor(this, R.color.teal_200))
            )
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
        lastLocation = location
    }

    private fun clearMap() {
        googleMap.clear()
        totalDistance = 0.0
        currentFare = BASE_FARE
        startTime = 0
        lastLocation = null
        traveledPath.clear()

        findViewById<TextView>(R.id.distanceTextView).text = String.format("Distance: %.3f km", 0.0)
        findViewById<TextView>(R.id.timeTextView).text = "Time: 0 min"
        findViewById<TextView>(R.id.fareTextView).text = String.format("Fare: %.3f DH", 0.0)
    }

    private fun saveRideDetails(onComplete: () -> Unit = {}) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000
            val rideData = hashMapOf(
                "distance" to totalDistance,
                "duration" to elapsedMinutes,
                "fare" to currentFare,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("drivers").document(userId).collection("rides")
                .add(rideData)
                .addOnSuccessListener {
                    Log.d("RideData", "Ride saved successfully!")
                    Toast.makeText(this, "Ride saved successfully!", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("RideDataError", "Error saving ride: ${e.message}")
                    Toast.makeText(this, "Error saving ride: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }
    }





    private fun showStopRideNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Ride Stopped")
            .setContentText(String.format("Fare: %.3f DH", currentFare))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Ride Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for ride notifications"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }
}
