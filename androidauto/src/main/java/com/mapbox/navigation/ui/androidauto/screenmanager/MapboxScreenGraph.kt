@file:JvmName("MapboxScreenGraph")

package com.mapbox.navigation.ui.androidauto.screenmanager

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.ACTIVE_GUIDANCE
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.ACTIVE_GUIDANCE_FEEDBACK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.ARRIVAL
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.FAVORITES
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.FAVORITES_FEEDBACK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.FREE_DRIVE
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.FREE_DRIVE_FEEDBACK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.GEO_DEEPLINK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.GEO_DEEPLINK_FEEDBACK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.NEEDS_LOCATION_PERMISSION
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.ROUTE_PREVIEW
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.ROUTE_PREVIEW_FEEDBACK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.SEARCH
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.SEARCH_FEEDBACK
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen.SETTINGS
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.ActiveGuidanceFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.ActiveGuidanceScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.ArrivalScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FavoritesFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FavoritesScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FreeDriveFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FreeDriveScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.GeoDeeplinkPlacesCarScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.GeoDeeplinkPlacesFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.NeedsLocationPermissionScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.RoutePreviewFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.RoutePreviewScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.RoutePreviewScreenFactory2
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.SearchPlacesFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.SearchPlacesScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.SettingsScreenFactory

/**
 * This is a predefined application that is designed to collect feedback from drivers.
 *
 * You can swap in your own [MapboxScreenFactory] to customize the experience.
 */
fun MapboxCarContext.prepareScreens() = apply {
    val mapboxCarContext = this
    mapboxScreenManager.putAll(
        NEEDS_LOCATION_PERMISSION
            to NeedsLocationPermissionScreenFactory(),
        SETTINGS
            to SettingsScreenFactory(mapboxCarContext),
        FREE_DRIVE
            to FreeDriveScreenFactory(mapboxCarContext),
        FREE_DRIVE_FEEDBACK
            to FreeDriveFeedbackScreenFactory(mapboxCarContext),
        SEARCH
            to SearchPlacesScreenFactory(mapboxCarContext),
        SEARCH_FEEDBACK
            to SearchPlacesFeedbackScreenFactory(mapboxCarContext),
        FAVORITES
            to FavoritesScreenFactory(mapboxCarContext),
        FAVORITES_FEEDBACK
            to FavoritesFeedbackScreenFactory(mapboxCarContext),
        GEO_DEEPLINK
            to GeoDeeplinkPlacesCarScreenFactory(mapboxCarContext),
        GEO_DEEPLINK_FEEDBACK
            to GeoDeeplinkPlacesFeedbackScreenFactory(mapboxCarContext),
        ROUTE_PREVIEW
            to RoutePreviewScreenFactory(mapboxCarContext),
        ROUTE_PREVIEW_FEEDBACK
            to RoutePreviewFeedbackScreenFactory(mapboxCarContext),
        ACTIVE_GUIDANCE
            to ActiveGuidanceScreenFactory(mapboxCarContext),
        ACTIVE_GUIDANCE_FEEDBACK
            to ActiveGuidanceFeedbackScreenFactory(mapboxCarContext),
        ARRIVAL
            to ArrivalScreenFactory(mapboxCarContext),
    )
}

@ExperimentalPreviewMapboxNavigationAPI
fun MapboxCarContext.prepareExperimentalRoutePreviewScreen() = apply {
    mapboxScreenManager[ROUTE_PREVIEW] = RoutePreviewScreenFactory2(mapboxCarContext = this)
}
