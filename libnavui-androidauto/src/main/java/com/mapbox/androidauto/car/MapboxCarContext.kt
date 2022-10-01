package com.mapbox.androidauto.car

import androidx.car.app.CarContext
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.car.feedback.core.CarFeedbackOptions
import com.mapbox.androidauto.car.feedback.core.CarFeedbackPollProvider
import com.mapbox.androidauto.car.navigation.speedlimit.SpeedLimitOptions
import com.mapbox.androidauto.car.preview.CarRouteOptionsInterceptor
import com.mapbox.androidauto.car.preview.CarRoutePreviewRequest
import com.mapbox.androidauto.car.search.CarPlaceSearchOptions
import com.mapbox.androidauto.car.settings.CarSettingsStorage
import com.mapbox.androidauto.deeplink.GeoDeeplinkPlacesListOnMapProvider
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(MapboxExperimental::class)
class MapboxCarContext(
    val carContext: CarContext,
    val lifecycleOwner: LifecycleOwner,
    val mapboxCarMap: MapboxCarMap,
    val feedbackPollProvider: CarFeedbackPollProvider = CarFeedbackPollProvider(),
    val carFeedbackOptions: CarFeedbackOptions = CarFeedbackOptions.Builder().build(),
    routeOptionsInterceptor: CarRouteOptionsInterceptor = CarRouteOptionsInterceptor { it },
    val carPlaceSearchOptions: CarPlaceSearchOptions = CarPlaceSearchOptions.Builder().build(),
) {
    val carSettingsStorage = CarSettingsStorage(carContext)
    val mapboxScreenManager = MapboxScreenManager(carContext, lifecycleOwner)

    val speedLimitOptions = MutableStateFlow(SpeedLimitOptions.Builder().build())

    val carRoutePreviewRequest = CarRoutePreviewRequest(routeOptionsInterceptor)

    // This is internal because it surfaces search objects which will likely change.
    internal var geoDeeplinkPlacesProvider: GeoDeeplinkPlacesListOnMapProvider? = null
}
