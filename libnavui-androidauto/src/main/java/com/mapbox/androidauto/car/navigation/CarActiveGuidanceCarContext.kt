package com.mapbox.androidauto.car.navigation

import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.navigation.lanes.CarLanesImageRenderer
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconOptions
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter

class CarActiveGuidanceCarContext(
    val mainCarContext: MainCarContext
) {
    /** MapCarContext **/
    val carContext = mainCarContext.carContext
    val mapboxCarMap = mainCarContext.mapboxCarMap
    val mapboxNavigation = mainCarContext.mapboxNavigation
    val distanceFormatter = mainCarContext.distanceFormatter

    /** NavigationCarContext **/
    private val carDistanceFormatter = CarDistanceFormatter(
        mapboxNavigation.navigationOptions.distanceFormatterOptions.unitType
    )
    private val carLaneImageGenerator = CarLanesImageRenderer(carContext)
    val navigationInfoMapper = CarNavigationInfoMapper(
        carContext.applicationContext,
        CarManeuverInstructionRenderer(),
        CarManeuverIconRenderer(CarManeuverIconOptions.Builder(carContext).build()),
        carLaneImageGenerator,
        carDistanceFormatter
    )
    val maneuverApi = mainCarContext.maneuverApi
    val tripProgressMapper = CarNavigationEtaMapper(
        carDistanceFormatter,
        MapboxTripProgressApi(TripProgressUpdateFormatter.Builder(carContext).build()),
    )
}
