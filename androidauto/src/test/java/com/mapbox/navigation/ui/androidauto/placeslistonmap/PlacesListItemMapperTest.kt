package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacesListItemMapperTest : MapboxRobolectricTestRunner() {

    private val placeMarkerRenderer: PlaceMarkerRenderer = mockk {
        every { renderMarker() } returns mockk {
            every { type } returns CarIcon.TYPE_CUSTOM
            every { icon } returns mockk {
                every { type } returns IconCompat.TYPE_BITMAP
            }
        }
    }

    private val mapper = PlacesListItemMapper(placeMarkerRenderer, UnitType.METRIC)

    @Test
    fun mapToItemList() {
        val location = Location.Builder().apply {
            latitude(37.8031596290125)
            longitude(-122.44783300404791)
        }.build()
        val places = listOf(
            PlaceRecord(
                "id",
                "name",
                Point.fromLngLat(-122.44783300404791, 37.8031596290125),
                "description",
                listOf(),
            ),
        )

        val result = mapper.mapToItemList(location, places, null)

        assertEquals(
            "[title: name, text count: 1, image: null, isBrowsable: false]",
            result.items.first().toString(),
        )
    }
}
