package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.CarContext
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.androidauto.navigation.lanes.CarLanesImageRenderer
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverIconOptions
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxJunctionApi

/**
 * This class helps with unit testing.
 */
internal class CarNavigationInfoServices {

    fun carNavigationEtaMapper(
        carContext: CarContext,
    ): CarNavigationEtaMapper {
        val mapboxTripProgressApi = mapboxTripProgressApi(carContext)
        return CarNavigationEtaMapper(mapboxTripProgressApi)
    }

    fun carNavigationInfoMapper(
        carContext: CarContext,
        mapboxNavigation: MapboxNavigation,
    ): CarNavigationInfoMapper {
        return CarNavigationInfoMapper(
            mapboxNavigation.navigationOptions.applicationContext,
            CarManeuverInstructionRenderer(),
            CarManeuverIconRenderer(CarManeuverIconOptions.Builder(carContext).build()),
            CarLanesImageRenderer(carContext),
        )
    }

    fun maneuverApi(mapboxNavigation: MapboxNavigation): MapboxManeuverApi {
        val options = mapboxNavigation.navigationOptions.distanceFormatterOptions
        val distanceFormatter = MapboxDistanceFormatter(options)
        return MapboxManeuverApi(distanceFormatter)
    }

    fun junctionApi(): MapboxJunctionApi {
        return MapboxJunctionApi()
    }

    fun mapUserStyleObserver() = MapUserStyleObserver()

    private fun mapboxTripProgressApi(carContext: CarContext): MapboxTripProgressApi {
        return MapboxTripProgressApi(TripProgressUpdateFormatter.Builder(carContext).build())
    }
}
