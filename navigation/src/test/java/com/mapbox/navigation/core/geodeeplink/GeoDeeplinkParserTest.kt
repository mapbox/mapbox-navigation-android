package com.mapbox.navigation.core.geodeeplink

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GeoDeeplinkParserTest(
    data: Pair<String, GeoDeeplink?>,
) {
    private val input: String = data.first
    private val expected: GeoDeeplink? = data.second

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Pair<String, GeoDeeplink?>> = arrayOf(
            // Successful cases
            "geo:37.788151,-122.407543" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = null,
            ),
            "geo:37.788151,-122.407543" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = null,
            ),
            "geo:37.788151, -122.407543" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = null,
            ),
            "geo:37.788151,%20-122.407543" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = null,
            ),
            "geo:37.79576,-122.39395?q=1 Ferry Building, San Francisco, CA 94111" to GeoDeeplink(
                point = Point.fromLngLat(-122.39395, 37.79576),
                placeQuery = "1 Ferry Building, San Francisco, CA 94111",
            ),
            "geo:0.0,-62.785138" to GeoDeeplink(
                point = Point.fromLngLat(-62.785138, 0.0),
                placeQuery = null,
            ),
            "geo:37.788151,0.0" to GeoDeeplink(
                point = Point.fromLngLat(0.0, 37.788151),
                placeQuery = null,
            ),
            "geo:0,0?q=%E5%93%81%E5%B7%9D%E5%8C%BA%E5%A4%A7%E4%BA%95%206-16-16%20%E3%83%A1%" +
                "E3%82%BE%E3%83%B3%E9%B9%BF%E5%B3%B6%E3%81%AE%E7%A2%A7201%4035.595404%2C139" +
                ".731737" to GeoDeeplink(
                point = Point.fromLngLat(139.731737, 35.595404),
                placeQuery = "品川区大井 6-16-16 メゾン鹿島の碧201",
            ),
            "geo:0,0?q=54.356152,18.642736(ul. 3 maja 12, 80-802 Gdansk, Poland)" to GeoDeeplink(
                point = Point.fromLngLat(18.642736, 54.356152),
                placeQuery = "ul. 3 maja 12, 80-802 Gdansk, Poland",
            ),
            "geo:0,0?q=1600 Amphitheatre Parkway, Mountain+View, California" to GeoDeeplink(
                point = null,
                placeQuery = "1600 Amphitheatre Parkway, Mountain View, California",
            ),
            "geo:0,0?q=Coffee Shop@37.757527,-122.392937" to GeoDeeplink(
                point = Point.fromLngLat(-122.392937, 37.757527),
                placeQuery = "Coffee Shop",
            ),

            // Failure cases return null
            "geo:0,0" to null,
            "geo:," to null,
            "geo:,35.595404" to null,
            "geo:,35.595404" to null,
        )
    }

    @Test
    fun `test geo deep links`() {
        val geoDeeplink = GeoDeeplinkParser.parse(input)

        assertEquals(expected, geoDeeplink)
    }
}
