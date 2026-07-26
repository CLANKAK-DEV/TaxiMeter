package com.myapp.taximeter.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.taximeter.data.FareRepository
import com.myapp.taximeter.data.SettingsRepository
import com.myapp.taximeter.data.TripRepository
import com.myapp.taximeter.data.local.TripEntity
import com.myapp.taximeter.data.model.ActiveFareProfile
import com.myapp.taximeter.data.model.DriverFareOverride
import com.myapp.taximeter.data.model.VehicleType
import com.myapp.taximeter.domain.EarningsCalculator
import com.myapp.taximeter.domain.EarningsSummary
import com.myapp.taximeter.domain.FareBreakdown
import com.myapp.taximeter.domain.FareEngine
import com.myapp.taximeter.domain.FareEngineInput
import com.myapp.taximeter.domain.TripCsvBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TripMetrics(
    val distanceKm: Double,
    val durationMinutes: Double,
    val waitingMinutes: Double
)

class MainViewModel(
    private val fareRepository: FareRepository,
    private val tripRepository: TripRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _activeProfile = MutableLiveData<ActiveFareProfile?>()
    val activeProfile: LiveData<ActiveFareProfile?> = _activeProfile

    private val _fareBreakdown = MutableLiveData<FareBreakdown?>()
    val fareBreakdown: LiveData<FareBreakdown?> = _fareBreakdown

    private val _tripHistory = MutableLiveData<List<TripEntity>>(emptyList())
    val tripHistory: LiveData<List<TripEntity>> = _tripHistory

    private var historyJob: Job? = null

    var currentVehicleType: VehicleType = VehicleType.ECONOMY
        private set

    var lastCountryCode: String = settingsRepository.defaultCountryCode ?: "MA"
        private set

    var lastCity: String? = settingsRepository.defaultCity
        private set

    fun init(countryCode: String?, city: String?) {
        val cc = countryCode ?: settingsRepository.defaultCountryCode ?: "MA"
        val cty = city ?: settingsRepository.defaultCity
        lastCountryCode = cc
        lastCity = cty
        loadActiveProfile(cc, cty, currentVehicleType)
        observeHistory()
    }

    fun changeVehicleType(type: VehicleType) {
        currentVehicleType = type
        loadActiveProfile(lastCountryCode, lastCity, type)
    }

    fun updateLocationContext(countryCode: String, city: String?) {
        lastCountryCode = countryCode
        lastCity = city
        settingsRepository.defaultCountryCode = countryCode
        settingsRepository.defaultCity = city
        loadActiveProfile(countryCode, city, currentVehicleType)
    }

    private fun loadActiveProfile(countryCode: String, city: String?, vehicleType: VehicleType) {
        viewModelScope.launch {
            val profile = fareRepository.getActiveProfile(countryCode, city, vehicleType)
            _activeProfile.postValue(profile)
        }
    }

    fun recalculateFare(
        metrics: TripMetrics,
        isNight: Boolean,
        isWeekend: Boolean,
        isAirportTrip: Boolean,
        trafficMultiplier: Double,
        surgeMultiplier: Double
    ) {
        val profile = _activeProfile.value ?: return
        val input = FareEngineInput(
            distanceKm = metrics.distanceKm,
            durationMinutes = metrics.durationMinutes,
            waitingMinutes = metrics.waitingMinutes,
            isNight = isNight,
            isWeekend = isWeekend,
            isAirportTrip = isAirportTrip,
            trafficMultiplier = trafficMultiplier,
            surgeMultiplier = surgeMultiplier,
            vehicleType = profile.vehicleType
        )
        val breakdown = FareEngine.calculateFare(profile.fareConfig, input)
        _fareBreakdown.value = breakdown
    }

    fun saveCompletedTrip(
        metrics: TripMetrics,
        breakdown: FareBreakdown,
        startTime: Long,
        endTime: Long,
        countryCode: String,
        countryName: String,
        city: String?,
        vehicleType: VehicleType,
        usdRate: Double?
    ) {
        viewModelScope.launch {
            val totalLocal = breakdown.total
            val totalUsd = usdRate?.let { rate -> if (rate > 0) totalLocal / rate else null }
            val entity = TripEntity(
                startTime = startTime,
                endTime = endTime,
                distanceKm = metrics.distanceKm,
                durationMinutes = metrics.durationMinutes,
                waitingMinutes = metrics.waitingMinutes,
                totalFareLocal = totalLocal,
                totalFareUsd = totalUsd,
                currencyCode = breakdown.currencyCode,
                currencySymbol = breakdown.currencySymbol,
                countryCode = countryCode,
                countryName = countryName,
                city = city,
                vehicleType = vehicleType.name,
                weatherSummary = null,
                polylineEncoded = null
            )
            tripRepository.saveTrip(entity)
        }
    }

    /**
     * Persists a driver-defined tariff override for the current country /
     * city / vehicle type and reloads the active fare profile so the meter
     * immediately reflects the new rates. Null fields fall back to the
     * country default.
     */
    fun saveRateOverride(
        baseFare: Double?,
        farePerKm: Double?,
        farePerMinute: Double?,
        waitingRatePerMinute: Double?,
        nightSurchargePercent: Double?
    ) {
        val cc = lastCountryCode
        val city = lastCity
        val vt = currentVehicleType
        viewModelScope.launch {
            fareRepository.saveDriverOverride(
                countryCode = cc,
                city = city,
                vehicleType = vt,
                override = DriverFareOverride(
                    countryCode = cc,
                    city = city,
                    vehicleType = vt,
                    baseFare = baseFare,
                    farePerKm = farePerKm,
                    farePerMinute = farePerMinute,
                    minimumFare = null,
                    waitingTimeRatePerMinute = waitingRatePerMinute,
                    nightSurchargePercent = nightSurchargePercent,
                    weekendSurchargePercent = null,
                    airportSurcharge = null
                )
            )
            val profile = fareRepository.getActiveProfile(cc, city, vt)
            _activeProfile.postValue(profile)
        }
    }

    /** Removes any driver override and restores the country default tariff. */
    fun clearRateOverride() {
        saveRateOverride(null, null, null, null, null)
    }

    /** Deletes every saved trip on this device. */
    fun clearTripHistory() {
        viewModelScope.launch {
            tripRepository.clearAll()
        }
    }

    private fun observeHistory() {
        if (historyJob != null) return
        historyJob = viewModelScope.launch {
            tripRepository.getRecentTrips(20).collectLatest { trips ->
                _tripHistory.postValue(trips)
            }
        }
    }

    // ─── Earnings dashboard ──────────────────────────────────────────────────

    private val _earningsSummary = MutableLiveData<EarningsSummary?>()
    val earningsSummary: LiveData<EarningsSummary?> = _earningsSummary

    /** Recomputes daily/weekly/monthly totals from every saved trip. */
    fun refreshEarnings() {
        viewModelScope.launch {
            val trips = tripRepository.getAllTrips().first()
            _earningsSummary.postValue(EarningsCalculator.summarize(trips, System.currentTimeMillis()))
        }
    }

    // ─── CSV export ──────────────────────────────────────────────────────────

    /**
     * Builds a CSV of the full trip history and delivers it on the main
     * thread. Passes null when there are no trips to export.
     */
    fun buildTripHistoryCsv(onReady: (String?) -> Unit) {
        viewModelScope.launch {
            val trips = tripRepository.getAllTrips().first()
            if (trips.isEmpty()) {
                onReady(null)
                return@launch
            }
            onReady(TripCsvBuilder.build(trips))
        }
    }
}

