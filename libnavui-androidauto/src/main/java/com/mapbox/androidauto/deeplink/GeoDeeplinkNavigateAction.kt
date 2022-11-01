package com.mapbox.androidauto.deeplink

import android.content.Intent
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.geodeeplink.GeoDeeplinkParser
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

class GeoDeeplinkNavigateAction(val mapboxCarContext: MapboxCarContext) {
    fun onNewIntent(intent: Intent): Boolean {
        val mapboxNavigation = MapboxNavigationApp.current()
            ?: return false
        val geoDeeplink = GeoDeeplinkParser.parse(intent.dataString)
            ?: return false
        return preparePlacesListOnMapScreen(mapboxNavigation, geoDeeplink)
    }

    private fun preparePlacesListOnMapScreen(
        mapboxNavigation: MapboxNavigation,
        geoDeeplink: GeoDeeplink
    ): Boolean {
        logAndroidAuto("GeoDeeplinkNavigateAction preparePlacesListOnMapScreen")
        val accessToken = mapboxNavigation.navigationOptions.accessToken
        checkNotNull(accessToken) {
            "GeoDeeplinkGeocoding requires an access token"
        }
        mapboxCarContext.geoDeeplinkPlacesProvider = GeoDeeplinkPlacesListOnMapProvider(
            GeoDeeplinkGeocoding(accessToken),
            geoDeeplink
        )
        MapboxScreenManager.push(MapboxScreen.GEO_DEEPLINK)
        return true
    }
}
