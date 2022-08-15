package com.mapbox.androidauto.car

import androidx.car.app.CarContext
import com.mapbox.androidauto.car.feedback.core.CarFeedbackPollProvider
import com.mapbox.androidauto.car.preview.CarRouteOptionsInterceptor
import com.mapbox.androidauto.car.search.CarPlaceSearchOptions
import com.mapbox.androidauto.car.settings.CarSettingsStorage
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.utils.internal.JobControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@OptIn(MapboxExperimental::class)
class MainCarContext(
    val carContext: CarContext,
    val mapboxCarMap: MapboxCarMap,
    val feedbackPollProvider: CarFeedbackPollProvider = CarFeedbackPollProvider(),
    val routeOptionsInterceptor: CarRouteOptionsInterceptor = CarRouteOptionsInterceptor { it },
    val carPlaceSearchOptions: CarPlaceSearchOptions = CarPlaceSearchOptions.Builder().build(),
) {
    val carSettingsStorage = CarSettingsStorage(carContext)

    val mapboxNavigation: MapboxNavigation by lazy {
        MapboxNavigationProvider.retrieve()
    }

    val distanceFormatter: DistanceFormatter by lazy {
        MapboxDistanceFormatter(
            mapboxNavigation.navigationOptions.distanceFormatterOptions
        )
    }

    val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(distanceFormatter)
    }

    fun getJobControl(): JobControl {
        val supervisorJob = SupervisorJob()
        val scope = CoroutineScope(supervisorJob + Dispatchers.Main)
        return JobControl(supervisorJob, scope)
    }
}
