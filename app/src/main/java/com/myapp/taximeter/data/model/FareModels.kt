package com.myapp.taximeter.data.model

import androidx.annotation.Keep

/** Supported vehicle categories that influence fare multipliers. */
enum class VehicleType {
    ECONOMY,
    COMFORT,
    PREMIUM,
    MOTO
}

@Keep
data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    /** Number of fractional digits, e.g. 2 for USD, 0 for JPY. */
    val decimals: Int
)

@Keep
data class FareConfig(
    val countryCode: String,            // ISO 3166‑1 alpha‑2 (e.g. "MA")
    val countryName: String,            // "Morocco"
    val majorCities: List<String>,      // e.g. ["Casablanca", "Rabat"]
    val currency: CurrencyInfo,
    val baseFare: Double,               // starting fee
    val farePerKm: Double,              // distance component
    val farePerMinute: Double,          // time component
    val minimumFare: Double,            // minimum charged fare
    val waitingTimeRatePerMinute: Double,
    val nightSurchargePercent: Double,  // 0.25 = +25%
    val weekendSurchargePercent: Double,// 0.10 = +10%
    val airportSurcharge: Double        // flat amount applied for airport trips
)

/** Optional driver‑specific override of the default fare config. */
@Keep
data class DriverFareOverride(
    val countryCode: String,
    val city: String?,
    val vehicleType: VehicleType,
    val baseFare: Double? = null,
    val farePerKm: Double? = null,
    val farePerMinute: Double? = null,
    val minimumFare: Double? = null,
    val waitingTimeRatePerMinute: Double? = null,
    val nightSurchargePercent: Double? = null,
    val weekendSurchargePercent: Double? = null,
    val airportSurcharge: Double? = null
)

/** Wrapper used by the UI to present which config is active. */
data class ActiveFareProfile(
    val source: ProfileSource,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val vehicleType: VehicleType,
    val fareConfig: FareConfig
)

enum class ProfileSource {
    GLOBAL_DEFAULT,
    DRIVER_OVERRIDE
}

