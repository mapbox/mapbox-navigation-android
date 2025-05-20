package com.mapbox.navigation.ui.androidauto.deeplink

import android.content.Intent
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.geodeeplink.GeoDeeplinkParser
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

@ExperimentalPreviewMapboxNavigationAPI
class GeoDeeplinkNavigateAction(val mapboxCarContext: MapboxCarContext) {
    fun onNewIntent(intent: Intent): Boolean {
        val geoDeeplink = GeoDeeplinkParser.parse(intent.dataString)
            ?: return false
        return preparePlacesListOnMapScreen(geoDeeplink)
    }

    private fun preparePlacesListOnMapScreen(
        geoDeeplink: GeoDeeplink,
    ): Boolean {
        logAndroidAuto("GeoDeeplinkNavigateAction preparePlacesListOnMapScreen")
        mapboxCarContext.geoDeeplinkPlacesProvider = GeoDeeplinkPlacesListOnMapProvider(
            GeoDeeplinkGeocoding(),
            geoDeeplink,
        )
        MapboxScreenManager.push(MapboxScreen.GEO_DEEPLINK)
        return true
    }
}
