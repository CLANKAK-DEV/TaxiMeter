package com.myapp.taximeter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.myapp.taximeter.data.FareRepository
import com.myapp.taximeter.data.SettingsRepository
import com.myapp.taximeter.data.TripRepository

class MainViewModelFactory(
    private val fareRepository: FareRepository,
    private val tripRepository: TripRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(fareRepository, tripRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

