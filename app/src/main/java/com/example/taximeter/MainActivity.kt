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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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

        // Initialize MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Start Ride Button
        findViewById<Button>(R.id.startRideButton).setOnClickListener {
            if (isRunning) stopRide() else startRide()
            updateRideButtonText()
        }

        // Logout Button
        findViewById<ImageButton>(R.id.logoutButton).setOnClickListener { logoutUser() }

        // Profile Button
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener { navigateToProfile() }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Fetch ride history
        fetchRideHistory()

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
            fetchRideHistory() // Fetch updated ride history after saving the ride.
        }
        showStopRideNotification()

        // Reset all state variables
        totalDistance = 0.0
        currentFare = BASE_FARE
        startTime = 0
        lastLocation = null
        traveledPath.clear()

        // Clear the map
        googleMap.clear()

        // Reset UI
        findViewById<TextView>(R.id.timeTextView).text = "Time: 0 min"
        findViewById<TextView>(R.id.distanceTextView).text = "Distance: 0.0 km"
        findViewById<TextView>(R.id.fareTextView).text = "Fare: $BASE_FARE DH"
    }

    private fun updateRideButtonText() {
        val button = findViewById<Button>(R.id.startRideButton)
        button.text = if (isRunning) "Stop Ride" else "Start Ride"
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
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

            // Update UI
            findViewById<TextView>(R.id.distanceTextView).text = "Distance: $totalDistance km"
            findViewById<TextView>(R.id.timeTextView).text =
                "Time: $elapsedMinutes min"  // Time Update
            findViewById<TextView>(R.id.fareTextView).text = "Fare: $currentFare DH"

            // Update Map
            val currentLatLng = LatLng(location.latitude, location.longitude)
            traveledPath.add(currentLatLng)
            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(traveledPath)
                    .color(ContextCompat.getColor(this, R.color.teal_200))
            )
            // Move the camera to the current location
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
        lastLocation = location
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
                    Toast.makeText(this, "Ride saved successfully!", Toast.LENGTH_SHORT).show()
                    onComplete() // Invoke the callback when save is successful.
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving ride: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun fetchRideHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("drivers").document(userId).collection("rides")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val rideList = documents.map { document ->
                        val distance = document.getDouble("distance") ?: 0.0
                        val duration = document.getLong("duration") ?: 0
                        val fare = document.getDouble("fare") ?: 0.0
                        "Distance: $distance km, Duration: $duration min, Fare: $fare DH"
                    }
                    displayRideHistory(rideList)
                }
        }
    }

    private fun displayRideHistory(rideList: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rideList)
        findViewById<ListView>(R.id.rideListView).adapter = adapter
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
            .setContentText("Your ride has stopped. Final Fare: $currentFare DH")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Ride Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications for ride status" }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}