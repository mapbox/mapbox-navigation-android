package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class MapboxNavigationViewModelFactory(
    private val dropInUIMapboxNavigationFactory: DropInUIMapboxNavigationFactory
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapboxNavigationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapboxNavigationViewModel(dropInUIMapboxNavigationFactory) as T
        }
        throw IllegalArgumentException("Unable to construct navigation view model.")
    }
}
