package com.myapp.taximeter

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
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

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

        MobileAds.initialize(this) {}

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermissions()
        checkLocationSettings()

        findViewById<Button>(R.id.startRideButton).setOnClickListener {
            if (isRunning) stopRide() else startRide()
        }

        findViewById<Button>(R.id.clearButton).setOnClickListener { clearMap() }

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

        val adView = AdView(this)
        adView.adUnitId = "ca-app-pub-2615779396386471/8218138423"
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360))

        val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
        adContainerView.removeAllViews()
        adContainerView.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        updateRideButtonText()  // initialize button text on launch
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

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            // Location settings are satisfied
        }
        task.addOnFailureListener {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun startRide() {
        clearMap() // Reset map and data before starting

        isRunning = true
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        currentFare = BASE_FARE
        lastLocation = null
        traveledPath.clear()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        // Request current location to set start point
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = locationResult.lastLocation
                if (location != null) {
                    val startLatLng = LatLng(location.latitude, location.longitude)
                    traveledPath.add(startLatLng)
                    lastLocation = location
                    updateFare(location) // Immediate UI update

                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(startLatLng)
                            .title("Start Point")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))

                    startLocationUpdates()
                } else {
                    Toast.makeText(this@MainActivity, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        }, Looper.getMainLooper())

        updateRideButtonText() // Update button text to "Stop Ride"
    }

    private fun stopRide() {
        if (!isRunning) return
        isRunning = false
        if (::locationCallback.isInitialized) stopLocationUpdates()

        lastLocation?.let {
            val stopLatLng = LatLng(it.latitude, it.longitude)
            googleMap.addMarker(
                MarkerOptions()
                    .position(stopLatLng)
                    .title("Stop Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            if (traveledPath.isNotEmpty()) {
                val bounds = LatLngBounds.Builder().apply {
                    traveledPath.forEach { include(it) }
                    include(stopLatLng)
                }.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            }
        } ?: Toast.makeText(this, "Unable to fetch the last location", Toast.LENGTH_SHORT).show()

        showStopRideNotification()
        updateRideButtonText() // Update button text to "Start Ride"
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
            val distance = it.distanceTo(location) / 1000.0 // in km
            totalDistance += distance
        }

        val elapsedMinutes = ((System.currentTimeMillis() - startTime) / 60000).toInt()
        currentFare = BASE_FARE + (totalDistance * FARE_PER_KM) + (elapsedMinutes * FARE_PER_MIN)

        findViewById<TextView>(R.id.distanceTextView).text = String.format("Distance: %.2f km", totalDistance)
        findViewById<TextView>(R.id.timeTextView).text = "Time: $elapsedMinutes min"
        findViewById<TextView>(R.id.fareTextView).text = String.format("Fare: %.2f DH", currentFare)

        traveledPath.add(LatLng(location.latitude, location.longitude))
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(traveledPath)
                .color(ContextCompat.getColor(this, R.color.teal_200))
        )
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))

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
        val btn = findViewById<Button>(R.id.startRideButton)
        btn.text = if (isRunning) "Stop Ride" else "Start Ride"
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
        val stopPendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Ride Completed")
            .setContentText("Your ride has ended. Fare: %.2f DH".format(currentFare))
            .setContentIntent(stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // MapView lifecycle methods:
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
