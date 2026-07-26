package com.myapp.taximeter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.myapp.taximeter.data.local.TripEntity
import com.myapp.taximeter.data.model.*
import com.myapp.taximeter.domain.*
import com.myapp.taximeter.ui.history.TripHistoryAdapter
import com.myapp.taximeter.ui.main.MainViewModel
import com.myapp.taximeter.ui.main.MainViewModelFactory
import com.myapp.taximeter.ui.main.TripMetrics
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ─── Enums & State ────────────────────────────────────────────────────────────

enum class RideState { AVAILABLE, ACTIVE, PAUSED }
enum class MapTapMode { NONE, SET_PICKUP, SET_DESTINATION }

/** Legacy UI model for Currency (back-compat with the simplified picker) */
data class Currency(val name: String, val code: String, val symbol: String, val flag: String)

/** Legacy UI model for CountryRate (back-compat with simplified picker) */
data class CountryRate(
    val country: String,
    val flag: String,
    val ratePerKmUsd: Double,
    val currency: Currency,
    val baseFareUsd: Double = ratePerKmUsd * 1.5,
    val farePerMinUsd: Double = ratePerKmUsd * 0.15
)

// ─── Country Rate Adapter ─────────────────────────────────────────────────────

class CountryRateAdapter(
    private var items: List<CountryRate>,
    private val onSelected: (CountryRate) -> Unit
) : RecyclerView.Adapter<CountryRateAdapter.VH>() {

    private var filtered = items.toMutableList()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val flag: TextView = view.findViewById(R.id.currencyFlag)
        val name: TextView = view.findViewById(R.id.currencyName)
        val sub: TextView  = view.findViewById(R.id.currencyCode)
        val rate: TextView = view.findViewById(R.id.currencySymbol)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_currency, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = filtered[position]
        holder.flag.text = r.flag
        holder.name.text = r.country
        holder.sub.text  = r.currency.code
        holder.rate.text = "$${String.format("%.2f", r.ratePerKmUsd)}/km"
        holder.itemView.setOnClickListener { onSelected(r) }
    }

    override fun getItemCount() = filtered.size

    fun filter(query: String) {
        filtered = if (query.isBlank()) items.toMutableList()
        else items.filter { it.country.contains(query, ignoreCase = true) || it.currency.code.contains(query, ignoreCase = true) }.toMutableList()
        notifyDataSetChanged()
    }
}

// ─── MainActivity ─────────────────────────────────────────────────────────────

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MainActivity"

        /** Below this speed the taxi is considered waiting (stuck/stopped). */
        private const val WAITING_SPEED_KMH = 5f

        /**
         * Longest inter-fix gap credited to waiting time. Fixes normally arrive
         * every 1-2 s; anything longer means GPS dropped out, and crediting the
         * outage would overbill the passenger.
         */
        private const val MAX_FIX_GAP_MS = 10_000L
    }

    // Dependency Injection / ViewModel
    private val viewModel: MainViewModel by viewModels {
        val app = application as TaxiMeterApp
        MainViewModelFactory(app.fareRepository, app.tripRepository, app.settingsRepository)
    }

    // UI Components
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fareTextView: TextView
    private lateinit var currencyTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var statusIndicator: View
    private lateinit var startRideButton: MaterialButton
    private lateinit var pauseButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var countryButton: MaterialButton
    private lateinit var mapHintText: TextView
    private lateinit var tripSummaryCard: CardView
    private lateinit var summaryContent: LinearLayout
    private lateinit var keepScreenOnSwitch: SwitchMaterial

    private val settingsRepository by lazy { (application as TaxiMeterApp).settingsRepository }

    // Location & Maps
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var myLocation: LatLng? = null
    private var pickupMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var currentTapMode = MapTapMode.NONE

    // State
    private var rideState = RideState.AVAILABLE
    private var startTime = 0L
    private var totalDistance = 0.0
    private var totalWaitingMinutes = 0.0
    private var lastLocation: Location? = null

    /** Monotonic (elapsedRealtime) millis of the previous GPS fix; 0 = none yet. */
    private var lastFixElapsedMs = 0L
    private var currentSpeed = 0f
    private var pausedTime = 0L
    private var pauseStartTime = 0L
    private var lastTripShareText: String? = null
    private val uiUpdateHandler = Handler(Looper.getMainLooper())

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATION_CHANNEL_ID = "ride_notifications"

    private val countryRates = listOf(
        CountryRate("United States",    "🇺🇸", 1.83, Currency("US Dollar",            "USD","$",  "🇺🇸")),
        CountryRate("United Kingdom",   "🇬🇧", 2.05, Currency("British Pound",        "GBP","£",  "🇬🇧")),
        CountryRate("France",           "🇫🇷", 1.62, Currency("Euro",                "EUR","€",  "🇪🇺")),
        CountryRate("Germany",          "🇩🇪", 2.16, Currency("Euro",                "EUR","€",  "🇪🇺")),
        CountryRate("Switzerland",      "🇨🇭", 4.14, Currency("Swiss Franc",         "CHF","Fr.","🇨🇭")),
        CountryRate("Norway",           "🇳🇴", 1.41, Currency("Norwegian Krone",      "NOK","kr", "🇳🇴")),
        CountryRate("Singapore",        "🇸🇬", 0.78, Currency("Singapore Dollar",      "SGD","S$", "🇸🇬")),
        CountryRate("Japan",            "🇯🇵", 3.01, Currency("Japanese Yen",         "JPY","¥",  "🇯🇵")),
        CountryRate("Australia",        "🇦🇺", 1.48, Currency("Australian Dollar",    "AUD","A$", "🇦🇺")),
        CountryRate("Canada",           "🇨🇦", 1.44, Currency("Canadian Dollar",      "CAD","C$", "🇨🇦")),
        CountryRate("Morocco",          "🇲🇦", 0.75, Currency("Moroccan Dirham",       "MAD","DH", "🇲🇦")),
        CountryRate("United Arab Emirates","🇦🇪",0.68,Currency("UAE Dirham",           "AED","د.إ","🇦🇪")),
        CountryRate("Saudi Arabia",     "🇸🇦", 1.33, Currency("Saudi Riyal",          "SAR","ر.س","🇸🇦")),
        CountryRate("Egypt",            "🇪🇬", 0.35, Currency("Egyptian Pound",        "EGP","E£", "🇪🇬")),
        CountryRate("Turkey",           "🇹🇷", 0.82, Currency("Turkish Lira",          "TRY","₺",  "🇹🇷")),
        CountryRate("Brazil",           "🇧🇷", 0.86, Currency("Brazilian Real",        "BRL","R$", "🇧🇷")),
        CountryRate("Mexico",           "🇲🇽", 0.49, Currency("Mexican Peso",          "MXN","$",  "🇲🇽")),
        CountryRate("India",            "🇮🇳", 0.28, Currency("Indian Rupee",          "INR","₹",  "🇮🇳")),
        CountryRate("China",            "🇨🇳", 0.36, Currency("Chinese Yuan",           "CNY","¥",  "🇨🇳")),
        CountryRate("South Africa",     "🇿🇦", 0.92, Currency("South African Rand",    "ZAR","R",  "🇿🇦"))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        setupListeners()
        setupLocation()
        createNotificationChannel()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Sync with architecture
        observeViewModel()
        viewModel.init(null, null)

        startUIUpdateLoop()
    }

    private fun initUI() {
        mapView = findViewById(R.id.mapView)
        fareTextView = findViewById(R.id.fareTextView)
        currencyTextView = findViewById(R.id.currencyTextView)
        timeTextView = findViewById(R.id.timeTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        speedTextView = findViewById(R.id.speedTextView)
        statusTextView = findViewById(R.id.statusTextView)
        statusIndicator = findViewById(R.id.statusIndicator)
        startRideButton = findViewById(R.id.startRideButton)
        pauseButton = findViewById(R.id.pauseButton)
        clearButton = findViewById(R.id.clearButton)
        countryButton = findViewById(R.id.countryButton)
        mapHintText = findViewById(R.id.mapHintText)
        tripSummaryCard = findViewById(R.id.tripSummaryCard)
        summaryContent = findViewById(R.id.summaryContent)
        keepScreenOnSwitch = findViewById(R.id.keepScreenOnSwitch)
        keepScreenOnSwitch.isChecked = settingsRepository.keepScreenOnDuringRide
    }

    private fun setupListeners() {
        startRideButton.setOnClickListener { handleStartStop() }
        pauseButton.setOnClickListener { handlePauseResume() }
        clearButton.setOnClickListener { confirmResetRide() }
        countryButton.setOnClickListener { showCountryPickerDialog() }

        findViewById<View>(R.id.locationFab).setOnClickListener { centerOnMyLocation() }
        findViewById<View>(R.id.pickupFab).setOnClickListener { enterMapTapMode(MapTapMode.SET_PICKUP) }
        findViewById<View>(R.id.destinationFab).setOnClickListener { enterMapTapMode(MapTapMode.SET_DESTINATION) }
        findViewById<View>(R.id.closeSummaryButton).setOnClickListener { tripSummaryCard.visibility = View.GONE }

        // Trip history, tariff editor, sharing, currency shortcut
        findViewById<View>(R.id.historyFab).setOnClickListener { showTripHistoryDialog() }
        findViewById<View>(R.id.settingsButton).setOnClickListener { showRateEditorDialog() }
        findViewById<View>(R.id.shareTripButton).setOnClickListener { shareLastTrip() }
        findViewById<View>(R.id.currencyButton).setOnClickListener { showCountryPickerDialog() }

        // Earnings dashboard + keep-screen-on toggle
        findViewById<View>(R.id.earningsButton).setOnClickListener { showEarningsDialog() }
        keepScreenOnSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.keepScreenOnDuringRide = isChecked
            applyKeepScreenOn()
        }

        // Trip-in-progress safeguard: confirm before leaving during metering
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (rideState != RideState.AVAILABLE) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.exit_during_trip_title)
                        .setMessage(R.string.exit_during_trip_body)
                        .setPositiveButton(R.string.exit_anyway) { _, _ -> finish() }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.activeProfile.observe(this) { profile ->
            if (profile != null) {
                currencyTextView.text = profile.fareConfig.currency.symbol
                countryButton.text = getFlagByCountryCode(profile.countryCode)
                updateFareDisplay()
            }
        }

        viewModel.fareBreakdown.observe(this) { breakdown ->
            if (breakdown != null) {
                fareTextView.text = String.format("%.2f", breakdown.total)
            }
        }
    }

    private fun handleStartStop() {
        when (rideState) {
            RideState.AVAILABLE -> startRide()
            RideState.ACTIVE, RideState.PAUSED -> stopRide()
        }
    }

    private fun startRide() {
        rideState = RideState.ACTIVE
        startTime = System.currentTimeMillis()
        totalDistance = 0.0
        totalWaitingMinutes = 0.0
        pausedTime = 0L
        lastLocation = null
        lastFixElapsedMs = 0L

        startRideButton.text = getString(R.string.stop_ride)
        startRideButton.setBackgroundColor(ContextCompat.getColor(this, R.color.button_stop))
        pauseButton.setIconResource(R.drawable.ic_pause)
        pauseButton.contentDescription = getString(R.string.pause_ride)
        pauseButton.visibility = View.VISIBLE
        updateStatusDisplay()
        applyKeepScreenOn()
    }

    private fun stopRide() {
        val endTime = System.currentTimeMillis()
        val finalFare = viewModel.fareBreakdown.value?.total ?: 0.0
        val profile = viewModel.activeProfile.value

        if (profile != null && totalDistance > 0.05) {
            val metrics = TripMetrics(totalDistance, (endTime - startTime - pausedTime) / 60000.0, totalWaitingMinutes)
            viewModel.fareBreakdown.value?.let { breakdown ->
                viewModel.saveCompletedTrip(
                    metrics = metrics,
                    breakdown = breakdown,
                    startTime = startTime,
                    endTime = endTime,
                    countryCode = profile.countryCode,
                    countryName = profile.countryName,
                    city = profile.city,
                    vehicleType = profile.vehicleType,
                    usdRate = 1.0 // Simple parity for now or fetch from FX repo
                )
            }

            // Build a shareable receipt for the just-completed trip
            val elapsed = endTime - startTime - pausedTime
            val durationStr = String.format("%02d:%02d", elapsed / 60000, (elapsed % 60000) / 1000)
            val dateStr = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(startTime))
            val sym = profile.fareConfig.currency.symbol
            lastTripShareText = getString(
                R.string.share_trip_template,
                dateStr,
                String.format("%.2f %s", finalFare, sym),
                String.format("%.2f", totalDistance),
                durationStr
            )

            showTripSummary(finalFare, endTime)
        }

        resetRide()
    }

    private fun handlePauseResume() {
        if (rideState == RideState.ACTIVE) {
            rideState = RideState.PAUSED
            pauseStartTime = System.currentTimeMillis()
            pauseButton.setIconResource(R.drawable.ic_play)
            pauseButton.contentDescription = getString(R.string.resume_ride)
            updateStatusDisplay()
        } else if (rideState == RideState.PAUSED) {
            rideState = RideState.ACTIVE
            pausedTime += (System.currentTimeMillis() - pauseStartTime)
            // Restart interval tracking so the pause itself is never billed as waiting.
            lastFixElapsedMs = 0L
            pauseButton.setIconResource(R.drawable.ic_pause)
            pauseButton.contentDescription = getString(R.string.pause_ride)
            updateStatusDisplay()
        }
    }

    private fun resetRide() {
        rideState = RideState.AVAILABLE
        startTime = 0L
        totalDistance = 0.0
        totalWaitingMinutes = 0.0
        pausedTime = 0L
        currentSpeed = 0f

        startRideButton.text = getString(R.string.start_ride)
        startRideButton.setBackgroundColor(ContextCompat.getColor(this, R.color.button_start))
        pauseButton.visibility = View.GONE
        updateStatusDisplay()
        updateUI()
        applyKeepScreenOn()
    }

    /**
     * Trip-in-progress safeguard: resetting while the meter is running
     * discards an unsaved fare, so it requires explicit confirmation.
     * When no ride is active, reset stays a single tap (unchanged behavior).
     */
    private fun confirmResetRide() {
        if (rideState == RideState.AVAILABLE) {
            resetRide()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.discard_trip_title)
            .setMessage(R.string.discard_trip_body)
            .setPositiveButton(R.string.discard) { _, _ -> resetRide() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Holds the screen awake only while metering and only if the toggle is on. */
    private fun applyKeepScreenOn() {
        val shouldKeepOn = rideState != RideState.AVAILABLE && keepScreenOnSwitch.isChecked
        if (shouldKeepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun updateStatusDisplay() {
        when (rideState) {
            RideState.AVAILABLE -> {
                statusTextView.text = getString(R.string.status_available)
                statusIndicator.setBackgroundResource(R.drawable.status_dot)
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_available)
            }
            RideState.ACTIVE -> {
                statusTextView.text = getString(R.string.status_on_trip)
                statusIndicator.setBackgroundResource(R.drawable.status_dot)
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_stop)
            }
            RideState.PAUSED -> {
                statusTextView.text = getString(R.string.status_paused)
                statusIndicator.setBackgroundResource(R.drawable.status_dot)
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorWarning)
            }
        }
    }

    private fun startUIUpdateLoop() {
        val r = object : Runnable {
            override fun run() {
                if (rideState != RideState.AVAILABLE) updateUI()
                uiUpdateHandler.postDelayed(this, 1000)
            }
        }
        uiUpdateHandler.post(r)
    }

    private fun updateUI() {
        val elapsed = when (rideState) {
            RideState.ACTIVE -> System.currentTimeMillis() - startTime - pausedTime
            RideState.PAUSED -> pauseStartTime - startTime - pausedTime
            else -> 0L
        }
        timeTextView.text = String.format("%02d:%02d", elapsed / 60000, (elapsed % 60000) / 1000)
        distanceTextView.text = String.format("%.2f", totalDistance)
        speedTextView.text = currentSpeed.roundToInt().toString()

        // Sync with architecture
        val metrics = TripMetrics(totalDistance, elapsed / 60000.0, totalWaitingMinutes)
        viewModel.recalculateFare(
            metrics = metrics,
            isNight = isNightTime(),
            isWeekend = isWeekend(),
            isAirportTrip = false, // Could add a toggle in UI
            trafficMultiplier = if (currentSpeed < 10) 1.2 else 1.0,
            surgeMultiplier = 1.0
        )
    }

    private fun updateFareDisplay() {
        val total = viewModel.fareBreakdown.value?.total ?: viewModel.activeProfile.value?.fareConfig?.baseFare ?: 0.0
        fareTextView.text = String.format("%.2f", total)
    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    handleNewLocation(location)
                }
            }
        }
        requestPermissions()
    }

    private fun handleNewLocation(location: Location) {
        myLocation = LatLng(location.latitude, location.longitude)
        if (rideState == RideState.ACTIVE) {
            lastLocation?.let { last ->
                val distance = location.distanceTo(last) / 1000.0
                if (distance > 0.002) { // 2m threshold to filter noise
                    totalDistance += distance
                }
            }
            currentSpeed = location.speed * 3.6f // m/s to km/h
            // Waiting time accrues by the real interval between fixes, read from the
            // fix's monotonic clock. The old code added a fixed 1 s per callback while
            // fixes arrive every 1-2 s — a systematic undercount that shortchanged the
            // driver. The gap is capped so a GPS outage (tunnel, parking garage) can't
            // dump its whole duration into "waiting" on the first fix back.
            val fixElapsedMs = location.elapsedRealtimeNanos / 1_000_000
            if (currentSpeed < WAITING_SPEED_KMH && lastFixElapsedMs > 0L) {
                val deltaMs = (fixElapsedMs - lastFixElapsedMs).coerceIn(0L, MAX_FIX_GAP_MS)
                totalWaitingMinutes += deltaMs / 60_000.0
            }
            lastFixElapsedMs = fixElapsedMs
            lastLocation = location
        }
        updateMapCamera(false)
    }

    private fun updateMapCamera(force: Boolean) {
        if (!::googleMap.isInitialized || myLocation == null) return
        if (force || rideState == RideState.ACTIVE) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation!!, 15f))
        }
    }

    private fun showTripSummary(fare: Double, endTime: Long) {
        summaryContent.removeAllViews()
        val elapsed = endTime - startTime - pausedTime
        val durationStr = String.format("%02d:%02d", elapsed / 60000, (elapsed % 60000) / 1000)
        val sym = viewModel.activeProfile.value?.fareConfig?.currency?.symbol ?: ""

        addSummaryRow(getString(R.string.summary_fare), "$sym ${String.format("%.2f", fare)}")
        addSummaryRow(getString(R.string.summary_distance), "${String.format("%.2f", totalDistance)} km")
        addSummaryRow(getString(R.string.summary_time), durationStr)

        val elapsedHours = elapsed / 3600000.0
        if (elapsedHours > 0.0) {
            val avgSpeed = (totalDistance / elapsedHours).roundToInt()
            addSummaryRow(getString(R.string.summary_avg_speed), "$avgSpeed km/h")
        }

        tripSummaryCard.visibility = View.VISIBLE
    }

    private fun addSummaryRow(label: String, value: String) {
        val row = LayoutInflater.from(this).inflate(R.layout.summary_row, summaryContent, false)
        row.findViewById<TextView>(R.id.labelText).text = label
        row.findViewById<TextView>(R.id.valueText).text = value
        summaryContent.addView(row)
    }

    // ─── Map Tap Mode ─────────────────────────────────────────────────────────

    private fun enterMapTapMode(mode: MapTapMode) {
        currentTapMode = mode
        mapHintText.visibility = View.VISIBLE
        mapHintText.text = getString(
            if (mode == MapTapMode.SET_PICKUP) R.string.tap_map_pickup else R.string.tap_map_destination
        )
        Toast.makeText(this, R.string.tap_map_toast, Toast.LENGTH_SHORT).show()
    }

    private fun handleMapTap(latLng: LatLng) {
        when (currentTapMode) {
            MapTapMode.SET_PICKUP -> {
                pickupMarker?.remove()
                pickupMarker = googleMap.addMarker(MarkerOptions()
                    .position(latLng).title(getString(R.string.pickup)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
                currentTapMode = MapTapMode.NONE
            }
            MapTapMode.SET_DESTINATION -> {
                destinationMarker?.remove()
                destinationMarker = googleMap.addMarker(MarkerOptions()
                    .position(latLng).title(getString(R.string.destination)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                currentTapMode = MapTapMode.NONE
            }
            else -> return
        }
        mapHintText.visibility = View.GONE
        drawRouteAndEstimate()
    }

    private fun drawRouteAndEstimate() {
        val p = pickupMarker?.position ?: return
        val d = destinationMarker?.position ?: return

        routePolyline?.remove()
        routePolyline = googleMap.addPolyline(PolylineOptions()
            .add(p, d).width(10f).color(ContextCompat.getColor(this, R.color.colorAccent)).pattern(listOf(Dash(20f), Gap(10f))))

        val distKm = haversineKm(p.latitude, p.longitude, d.latitude, d.longitude)
        val profile = viewModel.activeProfile.value?.fareConfig ?: return
        val estPrice = profile.baseFare + (distKm * profile.farePerKm)
        val sym = viewModel.activeProfile.value?.fareConfig?.currency?.symbol ?: ""

        Toast.makeText(
            this,
            getString(
                R.string.route_estimate_format,
                String.format("%.1f", distKm),
                sym,
                String.format("%.2f", estPrice)
            ),
            Toast.LENGTH_LONG
        ).show()

        val bounds = LatLngBounds.builder().include(p).include(d).build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ─── Trip History ─────────────────────────────────────────────────────────

    private fun showTripHistoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_history, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val rv = dialogView.findViewById<RecyclerView>(R.id.historyRecyclerView)
        val emptyState = dialogView.findViewById<View>(R.id.historyEmptyState)
        val tripsCount = dialogView.findViewById<TextView>(R.id.historyTripsCount)
        val totalKm = dialogView.findViewById<TextView>(R.id.historyTotalKm)
        val totalEarnings = dialogView.findViewById<TextView>(R.id.historyTotalEarnings)

        val adapter = TripHistoryAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val observer = Observer<List<TripEntity>> { trips ->
            adapter.submitList(trips)
            val isEmpty = trips.isEmpty()
            rv.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE

            tripsCount.text = trips.size.toString()
            totalKm.text = String.format("%.1f", trips.sumOf { it.distanceKm })

            val symbols = trips.map { it.currencySymbol }.distinct()
            totalEarnings.text = when {
                trips.isEmpty() -> "0.00"
                symbols.size == 1 ->
                    String.format("%.2f %s", trips.sumOf { it.totalFareLocal }, symbols.first())
                else -> getString(R.string.history_mixed_currencies)
            }
        }
        viewModel.tripHistory.observe(this, observer)
        dialog.setOnDismissListener { viewModel.tripHistory.removeObserver(observer) }

        dialogView.findViewById<View>(R.id.closeHistoryButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.exportCsvButton).setOnClickListener { exportTripHistoryCsv() }
        dialogView.findViewById<View>(R.id.clearHistoryButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_history_confirm_title)
                .setMessage(R.string.clear_history_confirm_body)
                .setPositiveButton(R.string.clear_all) { _, _ -> viewModel.clearTripHistory() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        dialog.show()
    }

    // ─── CSV Export ───────────────────────────────────────────────────────────

    /**
     * Exports the full trip history as CSV: a copy is written to app-specific
     * external storage (no permission needed) and the content is shared via
     * ACTION_SEND as text/csv so it can be mailed or saved to Drive/Files.
     */
    private fun exportTripHistoryCsv() {
        viewModel.buildTripHistoryCsv { csv ->
            if (csv == null) {
                Toast.makeText(this, R.string.csv_export_empty, Toast.LENGTH_SHORT).show()
                return@buildTripHistoryCsv
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val fileName = "taximeter_trips_$stamp.csv"
            try {
                val dir = File(getExternalFilesDir(null), "exports").apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeText(csv)
                Toast.makeText(
                    this,
                    getString(R.string.csv_saved_format, file.absolutePath),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                // Saving locally is best-effort; sharing below still works
                Log.w(TAG, "Could not write CSV copy to external files dir", e)
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, csv)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.export_csv)))
        }
    }

    // ─── Earnings Dashboard ───────────────────────────────────────────────────

    private fun showEarningsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_earnings, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val observer = Observer<EarningsSummary?> { summary ->
            if (summary != null) bindEarnings(dialogView, summary)
        }
        viewModel.earningsSummary.observe(this, observer)
        dialog.setOnDismissListener { viewModel.earningsSummary.removeObserver(observer) }

        dialogView.findViewById<View>(R.id.closeEarningsButton).setOnClickListener { dialog.dismiss() }

        viewModel.refreshEarnings()
        dialog.show()
    }

    private fun bindEarnings(root: View, summary: EarningsSummary) {
        fun money(value: Double): String =
            if (summary.currencySymbol != null)
                String.format(Locale.getDefault(), "%.2f %s", value, summary.currencySymbol)
            else getString(R.string.history_mixed_currencies)

        fun meta(stats: PeriodStats): String = getString(
            R.string.earnings_meta_format,
            stats.trips,
            String.format(Locale.getDefault(), "%.1f", stats.distanceKm)
        )

        root.findViewById<TextView>(R.id.earnTodayMeta).text = meta(summary.daily)
        root.findViewById<TextView>(R.id.earnTodayValue).text = money(summary.daily.earnings)
        root.findViewById<TextView>(R.id.earnWeekMeta).text = meta(summary.weekly)
        root.findViewById<TextView>(R.id.earnWeekValue).text = money(summary.weekly.earnings)
        root.findViewById<TextView>(R.id.earnMonthMeta).text = meta(summary.monthly)
        root.findViewById<TextView>(R.id.earnMonthValue).text = money(summary.monthly.earnings)

        root.findViewById<View>(R.id.earningsEmptyHint).visibility =
            if (summary.monthly.trips == 0) View.VISIBLE else View.GONE

        buildEarningsBars(
            root.findViewById(R.id.earningsChartContainer),
            summary.last7DayEarnings,
            summary.last7DayStarts
        )
    }

    /** Simple bar visualization built from plain views (no chart library). */
    private fun buildEarningsBars(container: LinearLayout, values: List<Double>, dayStarts: List<Long>) {
        container.removeAllViews()
        val maxValue = (values.maxOrNull() ?: 0.0).takeIf { it > 0.0 } ?: 1.0
        val barAreaPx = resources.getDimensionPixelSize(R.dimen.earnings_chart_height)
        val barWidthPx = resources.getDimensionPixelSize(R.dimen.earnings_bar_width)
        val minBarPx = resources.getDimensionPixelSize(R.dimen.earnings_bar_min_height)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        values.forEachIndexed { index, value ->
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }

            val barHeightPx = max((value / maxValue * barAreaPx).toInt(), minBarPx)
            val bar = View(this).apply {
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bar_earnings)
                alpha = if (value > 0.0) 1f else 0.35f
                layoutParams = LinearLayout.LayoutParams(barWidthPx, barHeightPx)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            val label = TextView(this).apply {
                text = dayStarts.getOrNull(index)?.let { dayFormat.format(Date(it)) } ?: ""
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                textSize = 10f
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = resources.getDimensionPixelSize(R.dimen.space_xs) }
            }

            column.addView(bar)
            column.addView(label)
            container.addView(column)
        }
    }

    // ─── Tariff Editor ────────────────────────────────────────────────────────

    private fun showRateEditorDialog() {
        val config = viewModel.activeProfile.value?.fareConfig
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)

        val baseFareEdit = dialogView.findViewById<TextInputEditText>(R.id.baseFareEdit)
        val farePerKmEdit = dialogView.findViewById<TextInputEditText>(R.id.farePerKmEdit)
        val farePerMinEdit = dialogView.findViewById<TextInputEditText>(R.id.farePerMinEdit)
        val waitingRateEdit = dialogView.findViewById<TextInputEditText>(R.id.waitingRateEdit)
        val nightSurchargeEdit = dialogView.findViewById<TextInputEditText>(R.id.nightSurchargeEdit)

        if (config != null) {
            baseFareEdit.setText(String.format(Locale.US, "%.2f", config.baseFare))
            farePerKmEdit.setText(String.format(Locale.US, "%.2f", config.farePerKm))
            farePerMinEdit.setText(String.format(Locale.US, "%.2f", config.farePerMinute))
            waitingRateEdit.setText(String.format(Locale.US, "%.2f", config.waitingTimeRatePerMinute))
            nightSurchargeEdit.setText(String.format(Locale.US, "%.0f", config.nightSurchargePercent * 100))
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val nightPercent = parseRateInput(nightSurchargeEdit)?.let { it / 100.0 }
                viewModel.saveRateOverride(
                    baseFare = parseRateInput(baseFareEdit),
                    farePerKm = parseRateInput(farePerKmEdit),
                    farePerMinute = parseRateInput(farePerMinEdit),
                    waitingRatePerMinute = parseRateInput(waitingRateEdit),
                    nightSurchargePercent = nightPercent
                )
                Toast.makeText(this, R.string.tariff_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.reset_to_default) { _, _ ->
                viewModel.clearRateOverride()
                Toast.makeText(this, R.string.tariff_reset, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /** Accepts "1.5" or "1,5"; rejects blanks, negatives, and non-finite values. */
    private fun parseRateInput(edit: TextInputEditText): Double? =
        edit.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }

    // ─── Trip Sharing ─────────────────────────────────────────────────────────

    private fun shareLastTrip() {
        val text = lastTripShareText
        if (text == null) {
            Toast.makeText(this, R.string.nothing_to_share, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    // ─── Country Picker ───────────────────────────────────────────────────────

    private fun showCountryPickerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_country_picker, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val rv = dialogView.findViewById<RecyclerView>(R.id.countryRecyclerView)
        val search = dialogView.findViewById<EditText>(R.id.countrySearch)

        val adapter = CountryRateAdapter(countryRates) { selected ->
            viewModel.updateLocationContext(codeToIso(selected.country), null)
            dialog.dismiss()
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter.filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        dialog.show()
    }

    private fun codeToIso(name: String) = when(name) {
        "United States" -> "US"; "United Kingdom" -> "GB"; "France" -> "FR"; "Germany" -> "DE"
        "Switzerland" -> "CH"; "Norway" -> "NO"; "Singapore" -> "SG"; "Japan" -> "JP"
        "Australia" -> "AU"; "Canada" -> "CA"; "Morocco" -> "MA"; "United Arab Emirates" -> "AE"
        "Saudi Arabia" -> "SA"; "Egypt" -> "EG"; "Turkey" -> "TR"; "Brazil" -> "BR"
        "Mexico" -> "MX"; "India" -> "IN"; "China" -> "CN"; "South Africa" -> "ZA"
        else -> "MA"
    }

    private fun getFlagByCountryCode(code: String) = when(code) {
        "US" -> "🇺🇸"; "GB" -> "🇬🇧"; "FR" -> "🇫🇷"; "DE" -> "🇩🇪"; "CH" -> "🇨🇭"; "NO" -> "🇳🇴"
        "SG" -> "🇸🇬"; "JP" -> "🇯🇵"; "AU" -> "🇦🇺"; "CA" -> "🇨🇦"; "MA" -> "🇲🇦"; "AE" -> "🇦🇪"
        "SA" -> "🇸🇦"; "EG" -> "🇪🇬"; "TR" -> "🇹🇷"; "BR" -> "🇧🇷"; "MX" -> "🇲🇽"; "IN" -> "🇮🇳"
        "CN" -> "🇨🇳"; "ZA" -> "🇿🇦"
        else -> "🏴"
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }

    private fun isWeekend(): Boolean {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Ride Activity", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun centerOnMyLocation() {
        if (!::googleMap.isInitialized) return // map not ready yet
        myLocation?.let { googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f)) }
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    /**
     * Previously the grant result was never handled, so location updates only
     * began after a full app restart when the user granted the permission from
     * the runtime dialog. Now metering starts immediately on grant.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
                enableMyLocationLayer()
            } else {
                Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableMyLocationLayer() {
        if (!::googleMap.isInitialized) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Match map surface to the app's dark theme at night
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            try {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
            } catch (e: Exception) {
                // Fall back to default map styling
                Log.w(TAG, "Could not apply dark map style", e)
            }
        }

        googleMap.setOnMapClickListener { handleMapTap(it) }
        googleMap.setPadding(0, 200, 0, 0)
        enableMyLocationLayer()
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        uiUpdateHandler.removeCallbacksAndMessages(null)
        // Stop GPS updates so the callback does not leak past the activity.
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}