package com.mapbox.navigation.ui.maps.internal.extensions

import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.maps.building.api.MapboxBuildingsApi
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Throws(BuildingError::class)
internal suspend fun MapboxBuildingsApi.queryBuildingToHighlight(
    point: Point,
): BuildingValue = suspendCancellableCoroutine { cont ->
    queryBuildingToHighlight(point) { expected ->
        expected.fold(
            { cont.resumeWithException(it) },
            { cont.resume(it) },
        )
    }
    cont.invokeOnCancellation { cancel() }
}
