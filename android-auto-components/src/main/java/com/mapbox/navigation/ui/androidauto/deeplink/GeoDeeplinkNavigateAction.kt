package com.mapbox.navigation.ui.androidauto.deeplink

import android.content.Intent
import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.geodeeplink.GeoDeeplinkParser
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog.logAndroidAutoWarning
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.search.CarSearchMode

@ExperimentalPreviewMapboxNavigationAPI
class GeoDeeplinkNavigateAction @VisibleForTesting internal constructor(
    val mapboxCarContext: MapboxCarContext,
    private val searchBoxProviderFactory: (GeoDeeplink) -> PlacesListOnMapProvider,
    private val geocodingProviderFactory: (GeoDeeplink) -> PlacesListOnMapProvider,
) {

    constructor(mapboxCarContext: MapboxCarContext) : this(
        mapboxCarContext = mapboxCarContext,
        searchBoxProviderFactory = {
            GeoDeeplinkSearchBoxPlacesListOnMapProvider(
                GeoDeeplinkSearchBox(),
                it,
            )
        },
        geocodingProviderFactory = {
            GeoDeeplinkPlacesListOnMapProvider(
                GeoDeeplinkGeocoding(),
                it,
            )
        },
    )

    fun onNewIntent(intent: Intent): Boolean {
        val geoDeeplink = GeoDeeplinkParser.parse(intent.dataString)
            ?: return false
        return preparePlacesListOnMapScreen(geoDeeplink)
    }

    private fun preparePlacesListOnMapScreen(
        geoDeeplink: GeoDeeplink,
    ): Boolean {
        logAndroidAuto("GeoDeeplinkNavigateAction preparePlacesListOnMapScreen")
        @Suppress("DEPRECATION")
        mapboxCarContext.geoDeeplinkPlacesProvider = when (mapboxCarContext.options.searchMode) {
            is CarSearchMode.SearchBox -> searchBoxProviderFactory(geoDeeplink)
            is CarSearchMode.Legacy -> geocodingProviderFactory(geoDeeplink)
            // CarSearchMode has a private constructor, so only the two modes above exist. It is
            // not sealed, so the compiler still needs this branch; it must not crash the session.
            else -> {
                logAndroidAutoWarning(
                    "Unknown search mode ${mapboxCarContext.options.searchMode}, using SearchBox",
                )
                searchBoxProviderFactory(geoDeeplink)
            }
        }
        // When the places of a previous deeplink are shown, show the new ones in their place.
        if (!mapboxCarContext.mapboxScreenManager.recreateTop(MapboxScreen.GEO_DEEPLINK)) {
            MapboxScreenManager.push(MapboxScreen.GEO_DEEPLINK)
        }
        return true
    }
}
