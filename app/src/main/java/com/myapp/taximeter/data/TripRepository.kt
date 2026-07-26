package com.myapp.taximeter.data

import com.myapp.taximeter.data.local.TripDao
import com.myapp.taximeter.data.local.TripEntity
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao
) {

    suspend fun saveTrip(entity: TripEntity): Long = tripDao.insertTrip(entity)

    fun getRecentTrips(limit: Int = 20): Flow<List<TripEntity>> = tripDao.getRecentTrips(limit)

    /** Every saved trip, newest first. Used by the earnings dashboard and CSV export. */
    fun getAllTrips(): Flow<List<TripEntity>> = tripDao.getAllTrips()

    suspend fun clearAll() = tripDao.clearAll()

    fun observeStats(): Triple<Flow<Int>, Flow<Double>, Flow<Double>> =
        Triple(tripDao.observeTripCount(), tripDao.observeTotalDistance(), tripDao.observeTotalEarnings())
}

