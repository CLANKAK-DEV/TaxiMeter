package com.myapp.taximeter.domain

import com.myapp.taximeter.data.GlobalFareDatabase
import com.myapp.taximeter.data.model.ActiveFareProfile
import com.myapp.taximeter.data.model.DriverFareOverride
import com.myapp.taximeter.data.model.FareConfig
import com.myapp.taximeter.data.model.VehicleType
import kotlin.math.max
import kotlin.math.round

data class FareEngineInput(
    val distanceKm: Double,
    val durationMinutes: Double,
    val waitingMinutes: Double,
    val isNight: Boolean,
    val isWeekend: Boolean,
    val isAirportTrip: Boolean,
    /** Traffic multiplier coming from UI (e.g. heavy traffic 1.2x). */
    val trafficMultiplier: Double,
    /** Surge pricing multiplier (e.g. 1.0 normal, 1.5 surge). */
    val surgeMultiplier: Double,
    val vehicleType: VehicleType
)

data class FareComponent(
    val label: String,
    val amount: Double
)

data class FareBreakdown(
    val currencyCode: String,
    val currencySymbol: String,
    val components: List<FareComponent>,
    val subtotal: Double,
    val total: Double
)

/**
 * Pure calculation engine – deterministic, testable, independent of Android.
 */
object FareEngine {

    /** Vehicle type multipliers relative to ECONOMY baseline. */
    private val vehicleMultipliers: Map<VehicleType, Double> = mapOf(
        VehicleType.ECONOMY to 1.0,
        VehicleType.COMFORT to 1.25,
        VehicleType.PREMIUM to 1.6,
        VehicleType.MOTO to 0.8
    )

    fun resolveEffectiveConfig(base: FareConfig, override: DriverFareOverride?): FareConfig {
        if (override == null) return base
        return base.copy(
            baseFare = override.baseFare ?: base.baseFare,
            farePerKm = override.farePerKm ?: base.farePerKm,
            farePerMinute = override.farePerMinute ?: base.farePerMinute,
            minimumFare = override.minimumFare ?: base.minimumFare,
            waitingTimeRatePerMinute = override.waitingTimeRatePerMinute ?: base.waitingTimeRatePerMinute,
            nightSurchargePercent = override.nightSurchargePercent ?: base.nightSurchargePercent,
            weekendSurchargePercent = override.weekendSurchargePercent ?: base.weekendSurchargePercent,
            airportSurcharge = override.airportSurcharge ?: base.airportSurcharge
        )
    }

    fun calculateFare(
        fareConfig: FareConfig,
        input: FareEngineInput
    ): FareBreakdown {
        val vMult = vehicleMultipliers[input.vehicleType] ?: 1.0

        val base = fareConfig.baseFare * vMult
        val distance = fareConfig.farePerKm * max(input.distanceKm, 0.0) * vMult
        val time = fareConfig.farePerMinute * max(input.durationMinutes, 0.0) * vMult
        val waiting = fareConfig.waitingTimeRatePerMinute * max(input.waitingMinutes, 0.0) * vMult

        val nightSurcharge = if (input.isNight) {
            (base + distance + time + waiting) * fareConfig.nightSurchargePercent
        } else 0.0

        val weekendSurcharge = if (input.isWeekend) {
            (base + distance + time + waiting) * fareConfig.weekendSurchargePercent
        } else 0.0

        val airport = if (input.isAirportTrip) fareConfig.airportSurcharge * vMult else 0.0

        val beforeMultipliers = base + distance + time + waiting +
            nightSurcharge + weekendSurcharge + airport

        val trafficUp = beforeMultipliers * (input.trafficMultiplier - 1.0)
        val surgeUp = (beforeMultipliers + trafficUp) * (input.surgeMultiplier - 1.0)

        val rawSubtotal = beforeMultipliers + trafficUp + surgeUp
        // Computed BEFORE clamping so the breakdown component reports the real
        // top-up (previously it was always 0.0 because it compared against the
        // already-clamped subtotal).
        val minimumFareAdjustment = max(0.0, fareConfig.minimumFare - rawSubtotal)
        val subtotal = rawSubtotal + minimumFareAdjustment

        val roundedTotal = roundToCurrency(subtotal, fareConfig.currency.decimals)

        val components = listOf(
            FareComponent("Base fare", base),
            FareComponent("Distance", distance),
            FareComponent("Time", time),
            FareComponent("Waiting", waiting),
            FareComponent("Night surcharge", nightSurcharge),
            FareComponent("Weekend surcharge", weekendSurcharge),
            FareComponent("Airport surcharge", airport),
            FareComponent("Traffic multiplier", trafficUp),
            FareComponent("Surge multiplier", surgeUp),
            FareComponent("Minimum fare adjustment", minimumFareAdjustment)
        )

        return FareBreakdown(
            currencyCode = fareConfig.currency.code,
            currencySymbol = fareConfig.currency.symbol,
            components = components,
            subtotal = subtotal,
            total = roundedTotal
        )
    }

    private fun roundToCurrency(value: Double, decimals: Int): Double {
        if (decimals <= 0) return round(value)
        val factor = Math.pow(10.0, decimals.toDouble())
        return round(value * factor) / factor
    }
}

/**
 * Convenience used by UI to build an ActiveFareProfile from a country code and
 * optional override. This is not strictly required but keeps MainViewModel cleaner.
 */
object FareProfileResolver {

    fun resolve(
        countryCode: String,
        city: String?,
        vehicleType: VehicleType,
        override: DriverFareOverride?
    ): ActiveFareProfile? {
        val base = GlobalFareDatabase.findByCountryCode(countryCode) ?: return null
        val effective = if (override != null) {
            FareEngine.resolveEffectiveConfig(base, override)
        } else {
            base
        }
        return ActiveFareProfile(
            source = if (override != null) com.myapp.taximeter.data.model.ProfileSource.DRIVER_OVERRIDE
            else com.myapp.taximeter.data.model.ProfileSource.GLOBAL_DEFAULT,
            countryCode = base.countryCode,
            countryName = base.countryName,
            city = city,
            vehicleType = vehicleType,
            fareConfig = effective
        )
    }
}

