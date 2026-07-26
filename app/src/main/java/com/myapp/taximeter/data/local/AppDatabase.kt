package com.myapp.taximeter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        FareOverrideEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun fareOverrideDao(): FareOverrideDao
    abstract fun userProfileDao(): UserProfileDao
}

