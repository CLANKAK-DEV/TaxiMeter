package com.myapp.taximeter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val durationMinutes: Double,
    val waitingMinutes: Double,
    val totalFareLocal: Double,
    val totalFareUsd: Double?,
    val currencyCode: String,
    val currencySymbol: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val vehicleType: String,
    val weatherSummary: String?,
    /** Encoded polyline of the route or null if not stored. */
    val polylineEncoded: String?
)

@Entity(tableName = "fare_overrides")
data class FareOverrideEntity(
    @PrimaryKey val key: String, // countryCode_city_vehicleType
    val countryCode: String,
    val city: String?,
    val vehicleType: String,
    val baseFare: Double?,
    val farePerKm: Double?,
    val farePerMinute: Double?,
    val minimumFare: Double?,
    val waitingTimeRatePerMinute: Double?,
    val nightSurchargePercent: Double?,
    val weekendSurchargePercent: Double?,
    val airportSurcharge: Double?
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 0L,
    val name: String?,
    val vehicleType: String,
    val countryCode: String?,
    val countryName: String?,
    val city: String?,
    val totalTrips: Int,
    val totalDistanceKm: Double,
    val totalEarningsLocal: Double,
    val preferredCurrencyCode: String?,
    val preferredLanguageCode: String?, // "en", "fr", "ar", "es", "zh"
    val darkThemeEnabled: Boolean
)

