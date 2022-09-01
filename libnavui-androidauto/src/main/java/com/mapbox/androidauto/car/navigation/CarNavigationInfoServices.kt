package com.mapbox.androidauto.car.navigation

import androidx.car.app.CarContext
import com.mapbox.androidauto.car.navigation.lanes.CarLanesImageRenderer
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconOptions
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter

/**
 * This class helps with unit testing.
 */
internal class CarNavigationInfoServices {

    fun carNavigationEtaMapper(
        carContext: CarContext,
        mapboxNavigation: MapboxNavigation,
    ): CarNavigationEtaMapper {
        val carDistanceFormatter = carDistanceFormatter(mapboxNavigation)
        val mapboxTripProgressApi = mapboxTripProgressApi(carContext)
        return CarNavigationEtaMapper(carDistanceFormatter, mapboxTripProgressApi)
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
            carDistanceFormatter(mapboxNavigation)
        )
    }

    fun maneuverApi(mapboxNavigation: MapboxNavigation): MapboxManeuverApi {
        val options = mapboxNavigation.navigationOptions.distanceFormatterOptions
        val distanceFormatter = MapboxDistanceFormatter(options)
        return MapboxManeuverApi(distanceFormatter)
    }

    fun mapUserStyleObserver() = MapUserStyleObserver()

    private fun carDistanceFormatter(mapboxNavigation: MapboxNavigation): CarDistanceFormatter {
        val unitType = mapboxNavigation.navigationOptions.distanceFormatterOptions.unitType
        return CarDistanceFormatter(unitType)
    }

    private fun mapboxTripProgressApi(carContext: CarContext): MapboxTripProgressApi {
        return MapboxTripProgressApi(TripProgressUpdateFormatter.Builder(carContext).build())
    }
}
