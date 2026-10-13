package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.model.CarIcon
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
            "[title: name, text count: 1, image: null, isBrowsable: false, isEnabled: true]",
            result.items.first().toString(),
        )
    }

    @Test
    fun `place without a description has no null text`() {
        val location = Location.Builder().apply {
            latitude(37.8031596290125)
            longitude(-122.44783300404791)
        }.build()
        val places = listOf(
            PlaceRecord(
                "id",
                "name",
                Point.fromLngLat(-122.44783300404791, 37.8031596290125),
                null,
                listOf(),
            ),
        )

        val result = mapper.mapToItemList(location, places, null)

        val row = result.items.first() as Row
        assertFalse(row.texts.single().toString().contains("null"))
    }

    @Test
    fun `rows with images fit a list template shown over the map`() {
        val result = mapper.mapToItemList(location(), places(), null, withRowImages = true)

        val row = result.items.single() as Row
        assertEquals(row.metadata!!.place!!.marker!!.icon, row.image)
        val listTemplate = ListTemplate.Builder().setSingleList(result).build()
        MapWithContentTemplate.Builder().setContentTemplate(listTemplate).build()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `rows without images fit the place list template`() {
        val result = mapper.mapToItemList(location(), places(), null)

        assertNull((result.items.single() as Row).image)
        androidx.car.app.navigation.model.PlaceListNavigationTemplate.Builder()
            .setItemList(result)
            .build()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `the place list template rejects rows with images`() {
        val result = mapper.mapToItemList(location(), places(), null, withRowImages = true)

        assertThrows(IllegalArgumentException::class.java) {
            androidx.car.app.navigation.model.PlaceListNavigationTemplate.Builder()
                .setItemList(result)
        }
    }

    private fun location() = Location.Builder().apply {
        latitude(37.8031596290125)
        longitude(-122.44783300404791)
    }.build()

    private fun places() = listOf(
        PlaceRecord(
            "id",
            "name",
            Point.fromLngLat(-122.44783300404791, 37.8031596290125),
            "description",
            listOf(),
        ),
    )
}
