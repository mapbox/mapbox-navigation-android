package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.navigation.ui.androidauto.search.GetPlacesError
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.search.PlaceRecordMapper
import com.mapbox.navigation.utils.internal.toPoint

internal class GeoDeeplinkSearchBoxPlacesListOnMapProvider(
    private val geoDeeplinkSearchBox: GeoDeeplinkSearchBox,
    private val geoDeeplink: GeoDeeplink,
) : PlacesListOnMapProvider {

    override suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>> {
        val origin = CarLocationProvider.getRegisteredInstance().waitForLocationOrNull()?.toPoint()

        val results = geoDeeplinkSearchBox.requestPlaces(geoDeeplink, origin)
            ?: return ExpectedFactory.createError(
                GetPlacesError("Error getting geo deeplink places.", null),
            )

        if (results.isEmpty()) {
            return ExpectedFactory.createError(
                GetPlacesError("No places found for geo deeplink.", null),
            )
        }

        return ExpectedFactory.createValue(
            results.map(PlaceRecordMapper::fromSearchResult),
        )
    }

    @Deprecated("Use coroutine scope cancellation instead.")
    override fun cancel() {
        geoDeeplinkSearchBox.cancel()
    }
}
