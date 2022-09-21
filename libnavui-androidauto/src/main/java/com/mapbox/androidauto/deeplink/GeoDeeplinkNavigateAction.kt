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
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.geodeeplink.GeoDeeplinkParser
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

class GeoDeeplinkNavigateAction(
    val mainCarContext: MainCarContext,
    val lifecycle: Lifecycle
) {
    fun onNewIntent(intent: Intent): Screen? {
        val mapboxNavigation = MapboxNavigationApp.current()
            ?: return null
        val geoDeeplink = GeoDeeplinkParser.parse(intent.dataString)
            ?: return null
        return preparePlacesListOnMapScreen(mapboxNavigation, geoDeeplink)
    }

    private fun preparePlacesListOnMapScreen(
        mapboxNavigation: MapboxNavigation,
        geoDeeplink: GeoDeeplink
    ): Screen {
        logAndroidAuto("GeoDeeplinkNavigateAction preparePlacesListOnMapScreen")
        val accessToken = mapboxNavigation.navigationOptions.accessToken
        checkNotNull(accessToken) {
            "GeoDeeplinkGeocoding requires an access token"
        }
        val placesProvider = GeoDeeplinkPlacesListOnMapProvider(
            GeoDeeplinkGeocoding(accessToken),
            geoDeeplink
        )
        val feedbackPoll = mainCarContext.feedbackPollProvider
            .getSearchFeedbackPoll(mainCarContext.carContext)
        return PlacesListOnMapScreen(
            mainCarContext,
            placesProvider,
            PlacesListItemMapper(
                PlaceMarkerRenderer(mainCarContext.carContext),
                mapboxNavigation
                    .navigationOptions
                    .distanceFormatterOptions
                    .unitType
            ),
            listOf(
                CarFeedbackAction(
                    mainCarContext.mapboxCarMap,
                    CarFeedbackSender(),
                    feedbackPoll,
                    placesProvider,
                )
            )
        )
    }
}
