package com.mapbox.navigation.dropin

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NavigationViewModelFactory(
    private val arg: String,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NavigationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NavigationViewModel(arg, application) as T
        }
        throw IllegalArgumentException("Unable to construct view model")
    }
}
