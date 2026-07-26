package com.myapp.taximeter

import android.app.Application
import androidx.room.Room
import com.myapp.taximeter.data.FareRepository
import com.myapp.taximeter.data.SettingsRepository
import com.myapp.taximeter.data.TripRepository
import com.myapp.taximeter.data.local.AppDatabase

class TaxiMeterApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var fareRepository: FareRepository
        private set

    lateinit var tripRepository: TripRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "taximeter.db"
        ).fallbackToDestructiveMigration().build()

        val prefs = getSharedPreferences("taximeter_prefs", MODE_PRIVATE)
        settingsRepository = SettingsRepository(prefs)

        fareRepository = FareRepository(database.fareOverrideDao())
        tripRepository = TripRepository(database.tripDao())
    }
}

