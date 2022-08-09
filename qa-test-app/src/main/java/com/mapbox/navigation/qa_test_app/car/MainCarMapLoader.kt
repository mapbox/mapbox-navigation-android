package com.mapbox.navigation.qa_test_app.car

import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.internal.logAndroidAutoFailure
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.navigation.ui.maps.NavigationStyles

@OptIn(MapboxExperimental::class)
class MainCarMapLoader : MapboxCarMapObserver {

    private var mapboxMap: MapboxMap? = null

    private val logMapError = object : OnMapLoadErrorListener {
        override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
            val errorData = "${eventData.type} ${eventData.message}"
            logAndroidAutoFailure("onMapLoadError $errorData")
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap()
        with(mapboxCarMapSurface) {
            logAndroidAuto("onAttached load style")
            mapSurface.getMapboxMap().loadStyleUri(
                mapStyleUri(carContext.isDarkMode),
                onStyleLoaded = {
                    logAndroidAuto("onAttached style loaded")
                },
                onMapLoadErrorListener = logMapError
            )
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapboxMap = null
    }

    fun mapStyleUri(isDarkMode: Boolean): String {
        return if (isDarkMode) {
            NavigationStyles.NAVIGATION_NIGHT_STYLE
        } else {
            NavigationStyles.NAVIGATION_DAY_STYLE
        }
    }

    // When the configuration changes, update the map style
    fun updateMapStyle(isDarkMode: Boolean) {
        mapboxMap?.loadStyleUri(
            mapStyleUri(isDarkMode),
            onStyleLoaded = { style ->
                logAndroidAuto("updateMapStyle styleAvailable ${style.styleURI}")
            },
            onMapLoadErrorListener = logMapError
        )
    }
}
