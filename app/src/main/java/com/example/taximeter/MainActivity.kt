package com.myapp.taximeter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class Currency(
    val name: String,
    val code: String,
    val symbol: String,
    val flag: String
)

data class RideSettings(
    val baseFare: Double = 2.5,
    val farePerKm: Double = 1.5,
    val farePerMinute: Double = 0.5,
    val waitingTimeRate: Double = 0.3,
    val nightSurcharge: Double = 0.2,
    val currency: Currency = Currency("Moroccan Dirham", "MAD", "DH", "🇲🇦")
)

data class TripSummary(
    val startTime: Long,
    val endTime: Long,
    val distance: Double,
    val fare: Double,
    val duration: Long,
    val avgSpeed: Double,
    val currency: Currency
)

class CurrencyAdapter(
    private val currencies: List<Currency>,
    private val onCurrencySelected: (Currency) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flag: TextView = view.findViewById(R.id.currencyFlag)
        val name: TextView = view.findViewById(R.id.currencyName)
        val code: TextView = view.findViewById(R.id.currencyCode)
        val symbol: TextView = view.findViewById(R.id.currencySymbol)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency, parent, false)
        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currency = currencies[position]
        holder.flag.text = currency.flag
        holder.name.text = currency.name
        holder.code.text = currency.code
        holder.symbol.text = currency.symbol

        holder.itemView.setOnClickListener {
            onCurrencySelected(currency)
        }
    }

    override fun getItemCount() = currencies.size
}

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var preferences: SharedPreferences
    private lateinit var uiUpdateHandler: Handler

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATION_CHANNEL_ID = "ride_notifications"
    private val NOTIFICATION_ID = 1

    private var rideSettings = RideSettings()
    private var rideState = RideState.AVAILABLE
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var totalDistance = 0.0
    private var currentFare = 0.0
    private var currentSpeed = 0f
    private var lastLocation: Location? = null
    private val traveledPath = mutableListOf<LatLng>()
    private var tripPolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private val tripHistory = mutableListOf<TripSummary>()

    // UI Components
    private lateinit var statusTextView: TextView
    private lateinit var statusIndicator: View
    private lateinit var timeTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var fareTextView: TextView
    private lateinit var currencyTextView: TextView
    private lateinit var startRideButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var pauseButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var currencyButton: MaterialButton
    private lateinit var historyFab: FloatingActionButton
    private lateinit var locationFab: FloatingActionButton
    private lateinit var tripSummaryCard: CardView

    // Comprehensive currency list
    private val availableCurrencies = listOf(
        // ==================== AFRICAN CURRENCIES ====================
        Currency("Moroccan Dirham", "MAD", "DH", "🇲🇦"),
        Currency("Egyptian Pound", "EGP", "E£", "🇪🇬"),
        Currency("Tunisian Dinar", "TND", "DT", "🇹🇳"),
        Currency("Algerian Dinar", "DZD", "DA", "🇩🇿"),
        Currency("South African Rand", "ZAR", "R", "🇿🇦"),
        Currency("Nigerian Naira", "NGN", "₦", "🇳🇬"),
        Currency("Kenyan Shilling", "KES", "KSh", "🇰🇪"),
        Currency("Ethiopian Birr", "ETB", "Br", "🇪🇹"),
        Currency("Ghanaian Cedi", "GHS", "₵", "🇬🇭"),
        Currency("Botswana Pula", "BWP", "P", "🇧🇼"),

        // ==================== MIDDLE EASTERN CURRENCIES ====================
        Currency("UAE Dirham", "AED", "د.إ", "🇦🇪"),
        Currency("Saudi Riyal", "SAR", "﷼", "🇸🇦"),
        Currency("Qatari Riyal", "QAR", "QR", "🇶🇦"),
        Currency("Kuwaiti Dinar", "KWD", "KD", "🇰🇼"),
        Currency("Bahraini Dinar", "BHD", "BD", "🇧🇭"),
        Currency("Omani Rial", "OMR", "ر.ع.", "🇴🇲"),
        Currency("Jordanian Dinar", "JOD", "JD", "🇯🇴"),
        Currency("Lebanese Pound", "LBP", "ل.ل", "🇱🇧"),
        Currency("Israeli Shekel", "ILS", "₪", "🇮🇱"),

        // ==================== EUROPEAN CURRENCIES ====================
        Currency("Euro", "EUR", "€", "🇪🇺"),
        Currency("British Pound", "GBP", "£", "🇬🇧"),
        Currency("Swiss Franc", "CHF", "CHF", "🇨🇭"),
        Currency("Norwegian Krone", "NOK", "kr", "🇳🇴"),
        Currency("Swedish Krona", "SEK", "kr", "🇸🇪"),
        Currency("Danish Krone", "DKK", "kr", "🇩🇰"),
        Currency("Polish Zloty", "PLN", "zł", "🇵🇱"),
        Currency("Czech Koruna", "CZK", "Kč", "🇨🇿"),
        Currency("Hungarian Forint", "HUF", "Ft", "🇭🇺"),
        Currency("Romanian Leu", "RON", "lei", "🇷🇴"),
        Currency("Bulgarian Lev", "BGN", "лв", "🇧🇬"),
        Currency("Croatian Kuna", "HRK", "kn", "🇭🇷"),
        Currency("Turkish Lira", "TRY", "₺", "🇹🇷"),
        Currency("Russian Ruble", "RUB", "₽", "🇷🇺"),
        Currency("Ukrainian Hryvnia", "UAH", "₴", "🇺🇦"),

        // ==================== AMERICAN CURRENCIES ====================
        Currency("US Dollar", "USD", "$", "🇺🇸"),
        Currency("Canadian Dollar", "CAD", "C$", "🇨🇦"),
        Currency("Mexican Peso", "MXN", "$", "🇲🇽"),
        Currency("Brazilian Real", "BRL", "R$", "🇧🇷"),
        Currency("Argentine Peso", "ARS", "$", "🇦🇷"),
        Currency("Chilean Peso", "CLP", "$", "🇨🇱"),
        Currency("Colombian Peso", "COP", "$", "🇨🇴"),
        Currency("Peruvian Sol", "PEN", "S/", "🇵🇪"),
        Currency("Paraguayan Guarani", "PYG", "₲", "🇵🇾"),

        // ==================== ASIAN CURRENCIES ====================
        Currency("Chinese Yuan", "CNY", "¥", "🇨🇳"),
        Currency("Japanese Yen", "JPY", "¥", "🇯🇵"),
        Currency("South Korean Won", "KRW", "₩", "🇰🇷"),
        Currency("Indian Rupee", "INR", "₹", "🇮🇳"),
        Currency("Pakistani Rupee", "PKR", "₨", "🇵🇰"),
        Currency("Bangladeshi Taka", "BDT", "৳", "🇧🇩"),
        Currency("Sri Lankan Rupee", "LKR", "Rs", "🇱🇰"),
        Currency("Thai Baht", "THB", "฿", "🇹🇭"),
        Currency("Vietnamese Dong", "VND", "₫", "🇻🇳"),
        Currency("Malaysian Ringgit", "MYR", "RM", "🇲🇾"),
        Currency("Singapore Dollar", "SGD", "S$", "🇸🇬"),
        Currency("Indonesian Rupiah", "IDR", "Rp", "🇮🇩"),
        Currency("Philippine Peso", "PHP", "₱", "🇵🇭"),
        Currency("Hong Kong Dollar", "HKD", "HK$", "🇭🇰"),
        Currency("Taiwan Dollar", "TWD", "NT$", "🇹🇼"),
        Currency("Macanese Pataca", "MOP", "MOP$", "🇲🇴"),

        // ==================== OCEANIAN CURRENCIES ====================
        Currency("Australian Dollar", "AUD", "A$", "🇦🇺"),
        Currency("New Zealand Dollar", "NZD", "NZ$", "🇳🇿"),
        Currency("Fijian Dollar", "FJD", "FJ$", "🇫🇯"),

        // ==================== CARIBBEAN CURRENCIES ====================
        Currency("Jamaican Dollar", "JMD", "J$", "🇯🇲"),
        Currency("Barbadian Dollar", "BBD", "Bds$", "🇧🇧"),
        Currency("Trinidad Dollar", "TTD", "TT$", "🇹🇹"),
        Currency("Bahaman Dollar", "BSD", "B$", "🇧🇸"),

        // ==================== CRYPTO CURRENCIES (OPTIONAL) ====================
        Currency("Bitcoin", "BTC", "₿", "₿"),
        Currency("Ethereum", "ETH", "Ξ", "Ξ"),
        Currency("Tether", "USDT", "₮", "₮"),

        // ==================== OTHER MAJOR CURRENCIES ====================
        Currency("Icelandic Krona", "ISK", "kr", "🇮🇸"),
        Currency("Armenian Dram", "AMD", "֏", "🇦🇲"),
        Currency("Georgian Lari", "GEL", "₾", "🇬🇪"),
        Currency("Azerbaijani Manat", "AZN", "₼", "🇦🇿"),
        Currency("Kazakhstani Tenge", "KZT", "₸", "🇰🇿"),
        Currency("Uzbekistani Som", "UZS", "so'm", "🇺🇿"),
        Currency("Mongolian Tugrik", "MNT", "₮", "🇲🇳"),
        Currency("Nepalese Rupee", "NPR", "Rs", "🇳🇵"),
        Currency("Bhutanese Ngultrum", "BTN", "Nu", "🇧🇹"),
        Currency("Maldivian Rufiyaa", "MVR", "Rf", "🇲🇻")
    )

    enum class RideState {
        AVAILABLE, ACTIVE, PAUSED, WAITING
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeComponents()
        setupMapView(savedInstanceState)
        setupLocationServices()
        setupEventListeners()
        setupNotifications()
        setupAds()
        loadSettings()

        // Start UI update loop
        startUIUpdateLoop()
    }

    private fun initializeComponents() {
        preferences = getSharedPreferences("taximeter_prefs", Context.MODE_PRIVATE)
        uiUpdateHandler = Handler(Looper.getMainLooper())

        // Initialize UI components
        statusTextView = findViewById(R.id.statusTextView)
        statusIndicator = findViewById(R.id.statusIndicator)
        timeTextView = findViewById(R.id.timeTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        speedTextView = findViewById(R.id.speedTextView)
        fareTextView = findViewById(R.id.fareTextView)
        currencyTextView = findViewById(R.id.currencyTextView)
        startRideButton = findViewById(R.id.startRideButton)
        clearButton = findViewById(R.id.clearButton)
        pauseButton = findViewById(R.id.pauseButton)
        settingsButton = findViewById(R.id.settingsButton)
        currencyButton = findViewById(R.id.currencyButton)
        historyFab = findViewById(R.id.historyFab)
        locationFab = findViewById(R.id.locationFab)
        tripSummaryCard = findViewById(R.id.tripSummaryCard)
    }

    private fun setupMapView(savedInstanceState: Bundle?) {
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupLocationServices() {
        checkLocationPermissions()
        checkLocationSettings()
    }

    private fun setupEventListeners() {
        startRideButton.setOnClickListener {
            when (rideState) {
                RideState.AVAILABLE -> startRide()
                RideState.ACTIVE -> stopRide()
                RideState.PAUSED -> resumeRide()
                RideState.WAITING -> stopRide()
            }
        }

        clearButton.setOnClickListener { clearMap() }
        pauseButton.setOnClickListener { pauseRide() }
        settingsButton.setOnClickListener { showSettingsDialog() }
        currencyButton.setOnClickListener { showCurrencyDialog() }
        historyFab.setOnClickListener { showTripHistory() }
        locationFab.setOnClickListener { moveToCurrentLocation() }

        findViewById<MaterialButton>(R.id.closeSummaryButton).setOnClickListener {
            tripSummaryCard.visibility = View.GONE
        }

        findViewById<MaterialButton>(R.id.shareTripButton).setOnClickListener {
            shareTripSummary()
        }
    }

    private fun setupNotifications() {
        createNotificationChannel()
    }

    private fun setupAds() {
        MobileAds.initialize(this) {}

        val adView = AdView(this)
        adView.adUnitId = "ca-app-pub-2615779396386471/8218138423"
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360))

        val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
        adContainerView.removeAllViews()
        adContainerView.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun loadSettings() {
        val baseFare = preferences.getFloat("base_fare", 2.5f).toDouble()
        val farePerKm = preferences.getFloat("fare_per_km", 1.5f).toDouble()
        val farePerMinute = preferences.getFloat("fare_per_minute", 0.5f).toDouble()
        val waitingTimeRate = preferences.getFloat("waiting_time_rate", 0.3f).toDouble()
        val nightSurcharge = preferences.getFloat("night_surcharge", 0.2f).toDouble()

        // Load saved currency
        val currencyName = preferences.getString("currency_name", "Moroccan Dirham") ?: "Moroccan Dirham"
        val currencyCode = preferences.getString("currency_code", "MAD") ?: "MAD"
        val currencySymbol = preferences.getString("currency_symbol", "DH") ?: "DH"
        val currencyFlag = preferences.getString("currency_flag", "🇲🇦") ?: "🇲🇦"
        val savedCurrency = Currency(currencyName, currencyCode, currencySymbol, currencyFlag)

        rideSettings = RideSettings(baseFare, farePerKm, farePerMinute, waitingTimeRate, nightSurcharge, savedCurrency)
        updateFareDisplay()
        updateCurrencyDisplay()
    }

    private fun saveSettings() {
        with(preferences.edit()) {
            putFloat("base_fare", rideSettings.baseFare.toFloat())
            putFloat("fare_per_km", rideSettings.farePerKm.toFloat())
            putFloat("fare_per_minute", rideSettings.farePerMinute.toFloat())
            putFloat("waiting_time_rate", rideSettings.waitingTimeRate.toFloat())
            putFloat("night_surcharge", rideSettings.nightSurcharge.toFloat())
            putString("currency_name", rideSettings.currency.name)
            putString("currency_code", rideSettings.currency.code)
            putString("currency_symbol", rideSettings.currency.symbol)
            putString("currency_flag", rideSettings.currency.flag)
            apply()
        }
    }

    private fun showCurrencyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_currency, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.currencyRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        recyclerView.adapter = CurrencyAdapter(availableCurrencies) { selectedCurrency ->
            rideSettings = rideSettings.copy(currency = selectedCurrency)
            saveSettings()
            updateCurrencyDisplay()
            updateFareDisplay()
            Toast.makeText(this, "Currency changed to ${selectedCurrency.name}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateCurrencyDisplay() {
        currencyTextView.text = rideSettings.currency.symbol
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()

        // Customize map appearance
        googleMap.uiSettings.apply {
            isMyLocationButtonEnabled = false // We have our own location button
            isCompassEnabled = true
            isZoomControlsEnabled = false
            isMapToolbarEnabled = false
        }

        // Set custom map style for better visibility
        try {
            googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
        } catch (e: Exception) {
            // Fallback to default style if custom style fails
        }

        moveToCurrentLocation()
    }

    private fun startUIUpdateLoop() {
        val updateRunnable = object : Runnable {
            override fun run() {
                if (rideState == RideState.ACTIVE) {
                    updateUI()
                }
                uiUpdateHandler.postDelayed(this, 1000) // Update every second
            }
        }
        uiUpdateHandler.post(updateRunnable)
    }

    private fun updateUI() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = when (rideState) {
            RideState.ACTIVE -> currentTime - startTime - pausedTime
            RideState.PAUSED -> currentTime - startTime - pausedTime
            else -> 0
        }

        val minutes = (elapsedTime / 60000).toInt()
        val seconds = ((elapsedTime % 60000) / 1000).toInt()

        timeTextView.text = String.format("%02d:%02d", minutes, seconds)
        distanceTextView.text = String.format("%.2f", totalDistance)
        speedTextView.text = currentSpeed.roundToInt().toString()

        updateFareDisplay()
        updateStatusDisplay()
    }

    private fun updateFareDisplay() {
        when (rideState) {
            RideState.AVAILABLE -> {
                currentFare = rideSettings.baseFare
                fareTextView.text = String.format("%.2f", currentFare)
            }
            RideState.ACTIVE, RideState.PAUSED -> {
                val elapsedMinutes = ((System.currentTimeMillis() - startTime - pausedTime) / 60000.0)
                val timeFare = elapsedMinutes * rideSettings.farePerMinute
                val distanceFare = totalDistance * rideSettings.farePerKm

                // Apply night surcharge if applicable
                val nightMultiplier = if (isNightTime()) 1 + rideSettings.nightSurcharge else 1.0

                currentFare = (rideSettings.baseFare + timeFare + distanceFare) * nightMultiplier
                fareTextView.text = String.format("%.2f", currentFare)
            }
            else -> {}
        }
        currencyTextView.text = rideSettings.currency.symbol
    }

    private fun updateStatusDisplay() {
        when (rideState) {
            RideState.AVAILABLE -> {
                statusTextView.text = "Available"
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_available)
                startRideButton.text = "START RIDE"
                startRideButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_start)
                pauseButton.visibility = View.GONE
            }
            RideState.ACTIVE -> {
                statusTextView.text = "In Trip"
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_active)
                startRideButton.text = "STOP RIDE"
                startRideButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_stop)
                pauseButton.visibility = View.VISIBLE
            }
            RideState.PAUSED -> {
                statusTextView.text = "Paused"
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_paused)
                startRideButton.text = "RESUME"
                startRideButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_start)
                pauseButton.visibility = View.VISIBLE
            }
            RideState.WAITING -> {
                statusTextView.text = "Waiting"
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_waiting)
                startRideButton.text = "STOP RIDE"
                startRideButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_stop)
                pauseButton.visibility = View.VISIBLE
            }
        }
    }

    private fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour <= 6
    }

    private fun startRide() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        clearMap()
        rideState = RideState.ACTIVE
        startTime = System.currentTimeMillis()
        pausedTime = 0
        totalDistance = 0.0
        currentFare = rideSettings.baseFare
        lastLocation = null
        traveledPath.clear()

        // Get current location and start tracking
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                locationResult.lastLocation?.let { location ->
                    val startLatLng = LatLng(location.latitude, location.longitude)
                    traveledPath.add(startLatLng)
                    lastLocation = location

                    // Add start marker
                    startMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(startLatLng)
                            .title("Trip Start")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 16f))
                    startLocationUpdates()
                }
            }
        }, Looper.getMainLooper())

        showNotification("Ride Started", "Your taxi ride has begun")
        updateUI()
    }

    private fun stopRide() {
        if (rideState == RideState.AVAILABLE) return

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime - pausedTime

        rideState = RideState.AVAILABLE
        stopLocationUpdates()

        lastLocation?.let { location ->
            val stopLatLng = LatLng(location.latitude, location.longitude)

            // Add end marker
            endMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(stopLatLng)
                    .title("Trip End")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            // Fit map to show entire route
            if (traveledPath.isNotEmpty()) {
                val bounds = LatLngBounds.Builder().apply {
                    traveledPath.forEach { include(it) }
                    include(stopLatLng)
                }.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }

        // Save trip to history
        val avgSpeed = if (duration > 0) (totalDistance / (duration / 3600000.0)) else 0.0
        val tripSummary = TripSummary(startTime, endTime, totalDistance, currentFare, duration, avgSpeed, rideSettings.currency)
        tripHistory.add(tripSummary)
        saveTripHistory()

        showTripSummary(tripSummary)
        showNotification("Ride Completed", "Total fare: %.2f ${rideSettings.currency.symbol}".format(currentFare))
        updateUI()
    }

    private fun pauseRide() {
        if (rideState != RideState.ACTIVE) return

        rideState = RideState.PAUSED
        pausedTime += System.currentTimeMillis() - startTime
        stopLocationUpdates()
        updateUI()

        showNotification("Ride Paused", "Trip is currently paused")
    }

    private fun resumeRide() {
        if (rideState != RideState.PAUSED) return

        rideState = RideState.ACTIVE
        startTime = System.currentTimeMillis()
        startLocationUpdates()
        updateUI()

        showNotification("Ride Resumed", "Trip has been resumed")
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationData(location)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun updateLocationData(location: Location) {
        lastLocation?.let { lastLoc ->
            val distance = lastLoc.distanceTo(location) / 1000.0 // Convert to km

            // Only add distance if movement is significant (reduces GPS noise)
            if (distance > 0.01) { // 10 meters minimum
                totalDistance += distance

                val currentLatLng = LatLng(location.latitude, location.longitude)
                traveledPath.add(currentLatLng)

                // Update polyline
                tripPolyline?.remove()
                tripPolyline = googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(traveledPath)
                        .color(ContextCompat.getColor(this, R.color.route_color))
                        .width(6f)
                )

                // Update camera to follow current location
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                )
            }
        }

        // Update speed
        currentSpeed = if (location.hasSpeed()) {
            location.speed * 3.6f // Convert m/s to km/h
        } else {
            0f
        }

        lastLocation = location
    }

    private fun clearMap() {
        googleMap.clear()
        traveledPath.clear()
        tripPolyline = null
        startMarker = null
        endMarker = null
        totalDistance = 0.0
        currentFare = rideSettings.baseFare
        currentSpeed = 0f
        pausedTime = 0
        updateUI()
    }

    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        val baseFareEdit = dialogView.findViewById<EditText>(R.id.baseFareEdit)
        val farePerKmEdit = dialogView.findViewById<EditText>(R.id.farePerKmEdit)
        val farePerMinEdit = dialogView.findViewById<EditText>(R.id.farePerMinEdit)
        val waitingRateEdit = dialogView.findViewById<EditText>(R.id.waitingRateEdit)
        val nightSurchargeEdit = dialogView.findViewById<EditText>(R.id.nightSurchargeEdit)

        // Populate current values
        baseFareEdit.setText(rideSettings.baseFare.toString())
        farePerKmEdit.setText(rideSettings.farePerKm.toString())
        farePerMinEdit.setText(rideSettings.farePerMinute.toString())
        waitingRateEdit.setText(rideSettings.waitingTimeRate.toString())
        nightSurchargeEdit.setText(rideSettings.nightSurcharge.toString())

        AlertDialog.Builder(this)
            .setTitle("Fare Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    rideSettings = rideSettings.copy(
                        baseFare = baseFareEdit.text.toString().toDouble(),
                        farePerKm = farePerKmEdit.text.toString().toDouble(),
                        farePerMinute = farePerMinEdit.text.toString().toDouble(),
                        waitingTimeRate = waitingRateEdit.text.toString().toDouble(),
                        nightSurcharge = nightSurchargeEdit.text.toString().toDouble()
                    )
                    saveSettings()
                    updateFareDisplay()
                    Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTripHistory() {
        if (tripHistory.isEmpty()) {
            Toast.makeText(this, "No trip history available", Toast.LENGTH_SHORT).show()
            return
        }

        val historyItems = tripHistory.takeLast(10).reversed().map { trip ->
            val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(trip.startTime))
            "$date - %.2f km - %.2f ${trip.currency.symbol}".format(trip.distance, trip.fare)
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Recent Trips")
            .setItems(historyItems) { _, which ->
                val selectedTrip = tripHistory.takeLast(10).reversed()[which]
                showTripSummary(selectedTrip)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showTripSummary(trip: TripSummary) {
        val summaryContent = findViewById<LinearLayout>(R.id.summaryContent)
        summaryContent.removeAllViews()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val durationMinutes = (trip.duration / 60000).toInt()

        addSummaryRow(summaryContent, "Start Time", dateFormat.format(Date(trip.startTime)))
        addSummaryRow(summaryContent, "End Time", dateFormat.format(Date(trip.endTime)))
        addSummaryRow(summaryContent, "Duration", "$durationMinutes minutes")
        addSummaryRow(summaryContent, "Distance", "%.2f km".format(trip.distance))
        addSummaryRow(summaryContent, "Average Speed", "%.1f km/h".format(trip.avgSpeed))
        addSummaryRow(summaryContent, "Total Fare", "%.2f ${trip.currency.symbol}".format(trip.fare))

        tripSummaryCard.visibility = View.VISIBLE
    }

    private fun addSummaryRow(parent: LinearLayout, label: String, value: String) {
        val rowView = layoutInflater.inflate(R.layout.summary_row, parent, false)
        rowView.findViewById<TextView>(R.id.labelText).text = label
        rowView.findViewById<TextView>(R.id.valueText).text = value
        parent.addView(rowView)
    }

    private fun shareTripSummary() {
        if (tripHistory.isEmpty()) return

        val lastTrip = tripHistory.last()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val durationMinutes = (lastTrip.duration / 60000).toInt()

        val shareText = """
            🚕 Taxi Trip Summary
            
            📅 Date: ${dateFormat.format(Date(lastTrip.startTime))}
            ⏱️ Duration: $durationMinutes minutes
            📍 Distance: %.2f km
            🏃 Avg Speed: %.1f km/h
            💰 Total Fare: %.2f ${lastTrip.currency.symbol}
            
            #TaxiMeter #RideComplete
        """.trimIndent().format(lastTrip.distance, lastTrip.avgSpeed, lastTrip.fare)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share Trip Summary"))
    }

    private fun saveTripHistory() {
        val editor = preferences.edit()
        val historyJson = tripHistory.takeLast(50).joinToString("|") { trip ->
            "${trip.startTime},${trip.endTime},${trip.distance},${trip.fare},${trip.duration},${trip.avgSpeed},${trip.currency.code},${trip.currency.symbol}"
        }
        editor.putString("trip_history", historyJson)
        editor.apply()
    }

    private fun loadTripHistory() {
        val historyJson = preferences.getString("trip_history", "") ?: ""
        if (historyJson.isNotEmpty()) {
            tripHistory.clear()
            historyJson.split("|").forEach { tripData ->
                val parts = tripData.split(",")
                if (parts.size >= 6) {
                    try {
                        val currency = if (parts.size >= 8) {
                            availableCurrencies.find { it.code == parts[6] }
                                ?: Currency("Unknown", parts[6], parts[7], "🌍")
                        } else {
                            rideSettings.currency
                        }

                        val trip = TripSummary(
                            parts[0].toLong(),
                            parts[1].toLong(),
                            parts[2].toDouble(),
                            parts[3].toDouble(),
                            parts[4].toLong(),
                            parts[5].toDouble(),
                            currency
                        )
                        tripHistory.add(trip)
                    } catch (e: NumberFormatException) {
                        // Skip invalid entries
                    }
                }
            }
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
            Toast.makeText(this, "Please enable location services for accurate tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission is required for taxi meter functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Taxi Meter Notifications"
            val descriptionText = "Notifications for ride updates and status"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_taxi)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        loadTripHistory()
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
        stopLocationUpdates()
        uiUpdateHandler.removeCallbacksAndMessages(null)
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}