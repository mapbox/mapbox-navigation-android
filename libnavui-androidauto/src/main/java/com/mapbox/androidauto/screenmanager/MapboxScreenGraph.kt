@file:JvmName("MapboxScreenGraph")

package com.mapbox.androidauto.screenmanager

import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.screenmanager.MapboxScreen.ACTIVE_GUIDANCE
import com.mapbox.androidauto.screenmanager.MapboxScreen.ACTIVE_GUIDANCE_FEEDBACK
import com.mapbox.androidauto.screenmanager.MapboxScreen.ARRIVAL
import com.mapbox.androidauto.screenmanager.MapboxScreen.FAVORITES
import com.mapbox.androidauto.screenmanager.MapboxScreen.FAVORITES_FEEDBACK
import com.mapbox.androidauto.screenmanager.MapboxScreen.FREE_DRIVE
import com.mapbox.androidauto.screenmanager.MapboxScreen.FREE_DRIVE_FEEDBACK
import com.mapbox.androidauto.screenmanager.MapboxScreen.GEO_DEEPLINK
import com.mapbox.androidauto.screenmanager.MapboxScreen.NEEDS_LOCATION_PERMISSION
import com.mapbox.androidauto.screenmanager.MapboxScreen.ROUTE_PREVIEW
import com.mapbox.androidauto.screenmanager.MapboxScreen.ROUTE_PREVIEW_FEEDBACK
import com.mapbox.androidauto.screenmanager.MapboxScreen.SEARCH
import com.mapbox.androidauto.screenmanager.MapboxScreen.SEARCH_FEEDBACK
import com.mapbox.androidauto.screenmanager.MapboxScreen.SETTINGS
import com.mapbox.androidauto.screenmanager.factories.ActiveGuidanceFeedbackScreenFactory
import com.mapbox.androidauto.screenmanager.factories.ActiveGuidanceScreenFactory
import com.mapbox.androidauto.screenmanager.factories.ArrivalScreenFactory
import com.mapbox.androidauto.screenmanager.factories.FavoritesFeedbackScreenFactory
import com.mapbox.androidauto.screenmanager.factories.FavoritesScreenFactory
import com.mapbox.androidauto.screenmanager.factories.FreeDriveFeedbackScreenFactory
import com.mapbox.androidauto.screenmanager.factories.FreeDriveScreenFactory
import com.mapbox.androidauto.screenmanager.factories.GeoDeeplinkPlacesCarScreenFactory
import com.mapbox.androidauto.screenmanager.factories.NeedsLocationPermissionScreenFactory
import com.mapbox.androidauto.screenmanager.factories.RoutePreviewFeedbackScreenFactory
import com.mapbox.androidauto.screenmanager.factories.RoutePreviewScreenFactory
import com.mapbox.androidauto.screenmanager.factories.SearchPlacesFeedbackScreenFactory
import com.mapbox.androidauto.screenmanager.factories.SearchPlacesScreenFactory
import com.mapbox.androidauto.screenmanager.factories.SettingsScreenFactory

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
        ROUTE_PREVIEW
            to RoutePreviewScreenFactory(mapboxCarContext),
        ROUTE_PREVIEW_FEEDBACK
            to RoutePreviewFeedbackScreenFactory(mapboxCarContext),
        ACTIVE_GUIDANCE
            to ActiveGuidanceScreenFactory(mapboxCarContext),
        ACTIVE_GUIDANCE_FEEDBACK
            to ActiveGuidanceFeedbackScreenFactory(mapboxCarContext),
        ARRIVAL
            to ArrivalScreenFactory(mapboxCarContext)
    )
}
