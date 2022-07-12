package com.mapbox.androidauto.deeplink

import android.content.Intent
import androidx.car.app.Screen
import androidx.lifecycle.Lifecycle
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.placeslistonmap.PlaceMarkerRenderer
import com.mapbox.androidauto.car.placeslistonmap.PlacesListItemMapper
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.geodeeplink.GeoDeeplinkParser

class GeoDeeplinkNavigateAction(
    val mainCarContext: MainCarContext,
    val lifecycle: Lifecycle
) {
    fun onNewIntent(intent: Intent): Screen? {
        val geoDeeplink = GeoDeeplinkParser.parse(intent.dataString)
            ?: return null
        return preparePlacesListOnMapScreen(geoDeeplink)
    }

    private fun preparePlacesListOnMapScreen(geoDeeplink: GeoDeeplink): Screen {
        logAndroidAuto("GeoDeeplinkNavigateAction preparePlacesListOnMapScreen")
        val accessToken = mainCarContext.mapboxNavigation.navigationOptions.accessToken
        checkNotNull(accessToken) {
            "GeoDeeplinkGeocoding requires an access token"
        }
        val placesProvider = GeoDeeplinkPlacesListOnMapProvider(
            mainCarContext.carContext,
            GeoDeeplinkGeocoding(accessToken),
            geoDeeplink
        )

        return PlacesListOnMapScreen(
            mainCarContext,
            placesProvider,
            PlacesListItemMapper(
                PlaceMarkerRenderer(mainCarContext.carContext),
                mainCarContext
                    .mapboxNavigation
                    .navigationOptions
                    .distanceFormatterOptions
                    .unitType
            ),
            listOf(
                CarFeedbackAction(mainCarContext.mapboxCarMap, CarFeedbackSender(), placesProvider)
            )
        )
    }
}
