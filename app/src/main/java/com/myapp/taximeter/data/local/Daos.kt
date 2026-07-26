package com.myapp.taximeter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(entity: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY startTime DESC LIMIT :limit")
    fun getRecentTrips(limit: Int): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT COUNT(*) FROM trips")
    fun observeTripCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceKm),0) FROM trips")
    fun observeTotalDistance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalFareLocal),0) FROM trips")
    fun observeTotalEarnings(): Flow<Double>

    @Query("DELETE FROM trips")
    suspend fun clearAll()
}

@Dao
interface FareOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(entity: FareOverrideEntity)

    @Query("SELECT * FROM fare_overrides WHERE key = :key LIMIT 1")
    suspend fun getOverrideByKey(key: String): FareOverrideEntity?
}

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>
}

