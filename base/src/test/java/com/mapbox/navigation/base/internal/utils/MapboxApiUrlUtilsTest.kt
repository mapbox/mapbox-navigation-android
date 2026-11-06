package com.mapbox.navigation.base.internal.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapboxApiUrlUtilsTest {

    @Test
    fun `https api mapbox com is a Mapbox API endpoint`() {
        assertTrue(MapboxApiUrlUtils.isMapboxApiEndpoint("https://api.mapbox.com/styles/v1/a/b"))
        assertTrue(MapboxApiUrlUtils.isMapboxApiEndpoint("HTTPS://API.MAPBOX.COM/styles"))
    }

    @Test
    fun `other hosts and schemes are not Mapbox API endpoints`() {
        listOf(
            "http://api.mapbox.com/styles",
            "https://test.example/styles",
            "//api.mapbox.com/styles",
            "",
        ).forEach {
            assertFalse(it, MapboxApiUrlUtils.isMapboxApiEndpoint(it))
        }
    }

    @Test
    fun `token is appended to a Mapbox API url`() {
        assertEquals(
            "https://api.mapbox.com/styles/v1/a/b/sprite.json?access_token=pk.123",
            MapboxApiUrlUtils.withMapboxAccessToken(
                "https://api.mapbox.com/styles/v1/a/b/sprite.json",
                "pk.123",
            ),
        )
    }

    @Test
    fun `token is added after existing query parameters, which keep their encoding`() {
        assertEquals(
            "https://api.mapbox.com/guidance-views/v1/1/jct/A?arrow_ids=B%2BC&image_format=png" +
                "&access_token=pk.123#top",
            MapboxApiUrlUtils.withMapboxAccessToken(
                "https://api.mapbox.com/guidance-views/v1/1/jct/A?arrow_ids=B%2BC&image_format=png#top",
                "pk.123",
            ),
        )
    }

    @Test
    fun `existing access token is replaced`() {
        assertEquals(
            "https://api.mapbox.com/a?b=1&access_token=pk.new",
            MapboxApiUrlUtils.withMapboxAccessToken(
                "https://api.mapbox.com/a?access_token=pk.old&b=1",
                "pk.new",
            ),
        )
    }

    @Test
    fun `token is encoded`() {
        assertEquals(
            "https://api.mapbox.com/a?access_token=pk.a%2Bb%26c%3D",
            MapboxApiUrlUtils.withMapboxAccessToken("https://api.mapbox.com/a", "pk.a+b&c="),
        )
    }

    @Test
    fun `url is unchanged for other hosts or schemes`() {
        listOf(
            "https://test.example/shield",
            "https://api.mapbox.com.test.example/shield?a=1",
            "http://api.mapbox.com/shield",
            "https://api.mapbox.com@test.example/shield",
            "ftp://api.mapbox.com/shield",
            "not a url",
        ).forEach {
            assertEquals(it, MapboxApiUrlUtils.withMapboxAccessToken(it, "pk.123"))
        }
    }

    @Test
    fun `url is unchanged for an empty token`() {
        val url = "https://api.mapbox.com/a?b=1"
        assertEquals(url, MapboxApiUrlUtils.withMapboxAccessToken(url, ""))
    }
}
