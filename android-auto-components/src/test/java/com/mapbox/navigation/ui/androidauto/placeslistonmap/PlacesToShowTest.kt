package com.mapbox.navigation.ui.androidauto.placeslistonmap

import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacesToShowTest {

    @Test
    fun `places without a coordinate are not shown`() {
        val places = listOf(place("a"), place("b", coordinate = null), place("c"))

        assertEquals(listOf("a", "c"), places.placesToShow(placeLimit = 10).map { it.id })
    }

    @Test
    fun `no more places than the limit are shown`() {
        val places = listOf(place("a"), place("b"), place("c"))

        assertEquals(listOf("a", "b"), places.placesToShow(placeLimit = 2).map { it.id })
    }

    @Test
    fun `places without a coordinate do not count towards the limit`() {
        val places = listOf(place("a", coordinate = null), place("b"), place("c"))

        assertEquals(listOf("b", "c"), places.placesToShow(placeLimit = 2).map { it.id })
    }

    private fun place(id: String, coordinate: Point? = Point.fromLngLat(1.0, 2.0)) =
        PlaceRecord(id = id, name = id, coordinate = coordinate)
}
