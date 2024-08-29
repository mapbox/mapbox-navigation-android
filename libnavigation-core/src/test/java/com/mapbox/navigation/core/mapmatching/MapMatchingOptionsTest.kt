@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.mapmatching

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMatchingOptionsTest {

    @Test
    fun `required params`() {
        val testCoordinates = "-117.17282,32.71204;-117.17288,32.71225"
        val accessToken = "test-token"
        val options = MapMatchingOptions.Builder()
            .coordinates(testCoordinates)
            .build()

        val url = options.toURL(accessToken)

        val httpUrl = url.toHttpUrl()
        assertEquals(
            testCoordinates,
            httpUrl.pathSegments[4],
        )
        assertEquals(
            accessToken,
            httpUrl.queryParameter("access_token"),
        )
    }

    @Test(expected = MapMatchingRequiredParameterError::class)
    fun `no coordinates`() {
        MapMatchingOptions.Builder().build()
    }

    @Test
    fun `default values`() {
        val options = createOptionsBuilderWithRequiredParams().build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "api.mapbox.com",
            httpUrl.host,
        )
        assertEquals(
            "matching",
            httpUrl.encodedPathSegments[0],
        )
        assertEquals(
            "v5",
            httpUrl.encodedPathSegments[1],
        )
        assertEquals(
            "mapbox",
            httpUrl.encodedPathSegments[2],
        )
        assertEquals(
            DirectionsCriteria.PROFILE_DRIVING,
            httpUrl.encodedPathSegments[3],
        )
        assertEquals(
            true,
            httpUrl.queryParameter("steps").toBoolean(),
        )
        assertEquals(
            "full",
            httpUrl.queryParameter("overview"),
        )
        assertEquals(
            DirectionsCriteria.GEOMETRY_POLYLINE6,
            httpUrl.queryParameter("geometries"),
        )

        assertNull(
            httpUrl.queryParameter("waypoints"),
        )
        assertNull(
            httpUrl.queryParameter("radiuses"),
        )
        assertNull(
            httpUrl.queryParameter("timestamps"),
        )
        assertNull(
            httpUrl.queryParameter("annotations"),
        )
        assertNull(
            httpUrl.queryParameter("language"),
        )
        assertNull(
            httpUrl.queryParameter("banner_instructions"),
        )
        assertNull(
            httpUrl.queryParameter("roundabout_exits"),
        )
        assertNull(
            httpUrl.queryParameter("voice_instructions"),
        )
        assertNull(
            httpUrl.queryParameter("waypoint_names"),
        )
        assertNull(
            httpUrl.queryParameter("ignore"),
        )
        assertNull(
            httpUrl.queryParameter("openlr_spec"),
        )
        assertNull(
            httpUrl.queryParameter("openlr_format"),
        )
    }

    @Test
    fun `set points to coordinates`() {
        val options = createOptionsBuilderWithRequiredParams()
            .coordinates(
                listOf(
                    Point.fromLngLat(1.0, 2.0),
                    Point.fromLngLat(3.0, 4.0),
                ),
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "1.0,2.0;3.0,4.0",
            httpUrl.pathSegments.last(),
        )
    }

    @Test
    fun `set open lr to coordinates`() {
        val options = createOptionsBuilderWithRequiredParams()
            .coordinates(
                "C1ZAJBE5bxNQ//RGDFITQA4A5wHdE2nk/w==",
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "C1ZAJBE5bxNQ//RGDFITQA4A5wHdE2nk/w==",
            httpUrl.pathSegments.last(),
        )
    }

    @Test
    fun waypoints() {
        val options = createOptionsBuilderWithRequiredParams()
            .waypoints(listOf(0, 1))
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "0;1",
            httpUrl.queryParameter("waypoints"),
        )
    }

    @Test
    fun profile() {
        val options = createOptionsBuilderWithRequiredParams()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            httpUrl.encodedPathSegments[3],
        )
    }

    @Test
    fun user() {
        val testUser = "mapbox-test"
        val options = createOptionsBuilderWithRequiredParams()
            .user(testUser)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            testUser,
            httpUrl.encodedPathSegments[2],
        )
    }

    @Test
    fun baseUrl() {
        val testBaseUrl = "http://test.mapbox.com"
        val options = createOptionsBuilderWithRequiredParams()
            .baseUrl(testBaseUrl)
            .build()

        val url = options.toURL("***")

        assertTrue(
            "Actual url is $url",
            url.startsWith(testBaseUrl),
        )
    }

    @Test
    fun radiuses() {
        val options = createOptionsBuilderWithRequiredParams()
            .radiuses(listOf(null, 4.0))
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            ";4.0",
            httpUrl.queryParameter("radiuses"),
        )
    }

    @Test
    fun timestamps() {
        val options = createOptionsBuilderWithRequiredParams()
            .timestamps(
                listOf(
                    1705574534,
                    1705574555,
                ),
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "1705574534;1705574555",
            httpUrl.queryParameter("timestamps"),
        )
    }

    @Test
    fun annotations() {
        val options = createOptionsBuilderWithRequiredParams()
            .annotations(
                listOf(
                    MapMatchingExtras.ANNOTATION_DURATION,
                    MapMatchingExtras.ANNOTATION_DISTANCE,
                    MapMatchingExtras.ANNOTATION_SPEED,
                    MapMatchingExtras.ANNOTATION_CONGESTION,
                    MapMatchingExtras.ANNOTATION_CONGESTION_NUMERIC,
                ),
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "duration,distance,speed,congestion,congestion_numeric",
            httpUrl.queryParameter("annotations"),
        )
    }

    @Test
    fun `single annotation`() {
        val options = createOptionsBuilderWithRequiredParams()
            .annotations(
                listOf(
                    MapMatchingExtras.ANNOTATION_SPEED,
                ),
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "speed",
            httpUrl.queryParameter("annotations"),
        )
    }

    @Test
    fun `empty annotations`() {
        val options = createOptionsBuilderWithRequiredParams()
            .annotations(
                listOf(),
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertNull(
            httpUrl.queryParameter("annotations"),
        )
    }

    @Test
    fun language() {
        val options = createOptionsBuilderWithRequiredParams()
            .language("en-GB")
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "en-GB",
            httpUrl.queryParameter("language"),
        )
    }

    @Test
    fun `banner instructions true`() {
        val options = createOptionsBuilderWithRequiredParams()
            .bannerInstructions(true)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "true",
            httpUrl.queryParameter("banner_instructions"),
        )
    }

    @Test
    fun `tidy true`() {
        val options = createOptionsBuilderWithRequiredParams()
            .tidy(true)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "true",
            httpUrl.queryParameter("tidy"),
        )
    }

    @Test
    fun `roundabout exits false`() {
        val options = createOptionsBuilderWithRequiredParams()
            .roundaboutExits(false)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "false",
            httpUrl.queryParameter("roundabout_exits"),
        )
    }

    @Test
    fun `voice instructions true`() {
        val options = createOptionsBuilderWithRequiredParams()
            .voiceInstructions(true)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "true",
            httpUrl.queryParameter("voice_instructions"),
        )
    }

    @Test
    fun `waypoints names`() {
        val options = createOptionsBuilderWithRequiredParams()
            .waypointNames(listOf(null, "two & two"))
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            ";two & two",
            httpUrl.queryParameter("waypoint_names"),
        )
    }

    @Test
    fun ignore() {
        val options = createOptionsBuilderWithRequiredParams()
            .ignore(
                listOf(
                    MapMatchingExtras.IGNORE_ACCESS,
                    MapMatchingExtras.IGNORE_ONEWAYS,
                    MapMatchingExtras.IGNORE_RESTRICTIONS,
                ),
            )
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "access,oneways,restrictions",
            httpUrl.queryParameter("ignore"),
        )
    }

    @Test
    fun `OpenLR spec`() {
        val options = createOptionsBuilderWithRequiredParams()
            .openlrSpec(MapMatchingExtras.OPENLR_SPEC_HERE)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "here",
            httpUrl.queryParameter("openlr_spec"),
        )
    }

    @Test
    fun `OpenLR format`() {
        val options = createOptionsBuilderWithRequiredParams()
            .openlrFormat(MapMatchingExtras.OPENLR_FORMAT_TOMTOM)
            .build()

        val url = options.toURL("***")

        val httpUrl = url.toHttpUrl()
        assertEquals(
            "tomtom",
            httpUrl.queryParameter("openlr_format"),
        )
    }
}

private fun createOptionsBuilderWithRequiredParams(): MapMatchingOptions.Builder {
    val testCoordinates = "-117.17282,32.71204;-117.17288,32.71225"
    return MapMatchingOptions.Builder()
        .coordinates(testCoordinates)
}
