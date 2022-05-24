package com.mapbox.navigation.ui.app.internal

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.utils.internal.configuration.NavigationConfigOwner
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceImpl
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceServicesImpl

@ExperimentalPreviewMapboxNavigationAPI
object SharedApp {
    private var isSetup = false

    fun setup(
        context: Context,
        audioGuidance: MapboxAudioGuidance? = null
    ) {
        if (isSetup) return
        isSetup = true

        val sharedAudioGuidance = audioGuidance ?: defaultAudioGuidance(context)
        MapboxNavigationApp.registerObserver(sharedAudioGuidance)
    }

    private fun defaultAudioGuidance(context: Context): MapboxAudioGuidance {
        return MapboxAudioGuidanceImpl(
            MapboxAudioGuidanceServicesImpl(),
            NavigationConfigOwner(context)
        ).also {
            it.dataStoreOwner = NavigationDataStoreOwner(context, DEFAULT_DATA_STORE_NAME)
        }
    }

    const val DEFAULT_DATA_STORE_NAME = "mapbox_navigation_preferences"
}
