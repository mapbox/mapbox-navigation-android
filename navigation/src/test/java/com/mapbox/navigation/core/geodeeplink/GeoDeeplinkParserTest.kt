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
            "geo:0,0?q=Porsche%20Service&sourceApplication%3DsmartApp" to GeoDeeplink(
                point = null,
                placeQuery = "Porsche Service",
            ),
            // URL-encoded '&' aka "%26" should not be ignored
            "geo:0,0?q=Cake%20%26%20Bake" to GeoDeeplink(
                point = null,
                placeQuery = "Cake & Bake",
            ),

            // A "%" that is not valid URL encoding is kept as it is
            "geo:0,0?q=50%off" to GeoDeeplink(
                point = null,
                placeQuery = "50%off",
            ),
            // Valid escapes and "+" are still decoded next to a stray "%"
            "geo:0,0?q=Caf%C3%A9+50%off" to GeoDeeplink(
                point = null,
                placeQuery = "Café 50%off",
            ),
            // A trailing or truncated "%" is kept, lowercase escapes are decoded
            "geo:0,0?q=100%" to GeoDeeplink(point = null, placeQuery = "100%"),
            "geo:0,0?q=a%2" to GeoDeeplink(point = null, placeQuery = "a%2"),
            "geo:0,0?q=Caf%c3%a9" to GeoDeeplink(point = null, placeQuery = "Café"),
            // The coordinate range limits are inclusive
            "geo:90,180" to GeoDeeplink(point = Point.fromLngLat(180.0, 90.0), placeQuery = null),
            "geo:-90,-180" to GeoDeeplink(
                point = Point.fromLngLat(-180.0, -90.0),
                placeQuery = null,
            ),
            // RFC 5870 parameters before the query are ignored, the query is kept
            "geo:37.788151,-122.407543;u=35?q=Cafe" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = "Cafe",
            ),
            // The scheme is case-insensitive
            "GEO:37.788151,-122.407543" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = null,
            ),
            // RFC 5870 parameters after ';' are ignored
            "geo:37.788151,-122.407543;u=35" to GeoDeeplink(
                point = Point.fromLngLat(-122.407543, 37.788151),
                placeQuery = null,
            ),
            // Out-of-range coordinates in the query are ignored, the place name is kept
            "geo:0,0?q=Cafe@95,10" to GeoDeeplink(
                point = null,
                placeQuery = "Cafe",
            ),

            // Failure cases return null
            "geo:91,10" to null,
            "geo:10,181" to null,
            "geo:-90.5,10" to null,
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
