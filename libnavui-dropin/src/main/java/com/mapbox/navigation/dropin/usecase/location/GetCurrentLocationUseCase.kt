package com.mapbox.navigation.dropin.usecase.location

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.dropin.extensions.coroutines.getLastLocation
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Use case for retrieving latest device location.
 */
@SuppressLint("MissingPermission")
class GetCurrentLocationUseCase(
    private val navigation: MapboxNavigation,
    private val locationBehavior: LocationBehavior,
    dispatcher: CoroutineDispatcher
) : UseCase<Unit, Location>(dispatcher) {

    override suspend fun execute(parameters: Unit): Location {
        var location = locationBehavior.locationLiveData.value
        if (location != null) return location

        val locationEngine = navigation.navigationOptions.locationEngine
        location = locationEngine.getLastLocation().lastLocation
        if (location == null) throw Error("Unable to get Location from LocationEngine")

        return location
    }
}
