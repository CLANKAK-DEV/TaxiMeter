package com.myapp.taximeter.data

import com.myapp.taximeter.data.local.FareOverrideDao
import com.myapp.taximeter.data.local.FareOverrideEntity
import com.myapp.taximeter.data.model.DriverFareOverride
import com.myapp.taximeter.data.model.VehicleType
import com.myapp.taximeter.domain.FareProfileResolver

class FareRepository(
    private val fareOverrideDao: FareOverrideDao
) {

    private fun buildKey(countryCode: String, city: String?, vehicleType: VehicleType): String {
        val c = city?.lowercase() ?: "any"
        return "${countryCode.uppercase()}_${c}_${vehicleType.name}"
    }

    suspend fun getActiveProfile(
        countryCode: String,
        city: String?,
        vehicleType: VehicleType
    ) = FareProfileResolver.resolve(
        countryCode = countryCode,
        city = city,
        vehicleType = vehicleType,
        override = getDriverOverride(countryCode, city, vehicleType)
    )

    private suspend fun getDriverOverride(
        countryCode: String,
        city: String?,
        vehicleType: VehicleType
    ): DriverFareOverride? {
        val key = buildKey(countryCode, city, vehicleType)
        val entity = fareOverrideDao.getOverrideByKey(key) ?: return null
        return DriverFareOverride(
            countryCode = entity.countryCode,
            city = entity.city,
            vehicleType = vehicleType,
            baseFare = entity.baseFare,
            farePerKm = entity.farePerKm,
            farePerMinute = entity.farePerMinute,
            minimumFare = entity.minimumFare,
            waitingTimeRatePerMinute = entity.waitingTimeRatePerMinute,
            nightSurchargePercent = entity.nightSurchargePercent,
            weekendSurchargePercent = entity.weekendSurchargePercent,
            airportSurcharge = entity.airportSurcharge
        )
    }

    suspend fun saveDriverOverride(
        countryCode: String,
        city: String?,
        vehicleType: VehicleType,
        override: DriverFareOverride
    ) {
        val key = buildKey(countryCode, city, vehicleType)
        val entity = FareOverrideEntity(
            key = key,
            countryCode = countryCode,
            city = city,
            vehicleType = vehicleType.name,
            baseFare = override.baseFare,
            farePerKm = override.farePerKm,
            farePerMinute = override.farePerMinute,
            minimumFare = override.minimumFare,
            waitingTimeRatePerMinute = override.waitingTimeRatePerMinute,
            nightSurchargePercent = override.nightSurchargePercent,
            weekendSurchargePercent = override.weekendSurchargePercent,
            airportSurcharge = override.airportSurcharge
        )
        fareOverrideDao.upsertOverride(entity)
    }
}

