package com.mapbox.navigation.tripdata.shield.model

import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.ShieldSpriteToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.generateSpriteSheetUrl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RouteShieldToDownloadTest {

    @Before
    fun setUp() {
        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns ACCESS_TOKEN
    }

    @After
    fun tearDown() {
        unmockkStatic(MapboxOptions::class)
    }

    @Test
    fun `test mapbox designed url generation`() {
        val toDownload = RouteShieldToDownload.MapboxDesign(
            shieldSpriteToDownload = ShieldSpriteToDownload("userId", "styleId"),
            mapboxShield = mockk {
                every { baseUrl() } returns "https://mapbox.base.url"
                every { name() } returns "default"
                every { displayRef() } returns "RT 82"
                every { textColor() } returns "black"
            },
            legacyFallback = null,
        )
        val expected =
            "https://mapbox.base.url/userId/styleId/sprite/default-5?access_token=$ACCESS_TOKEN"

        val actual = toDownload.generateUrl(
            mockk { every { spriteName() } returns "default-5" },
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `test mapbox designed sprite sheet url generation`() {
        val toDownload = RouteShieldToDownload.MapboxDesign(
            shieldSpriteToDownload = ShieldSpriteToDownload("userId", "styleId"),
            mapboxShield = mockk {
                every { baseUrl() } returns "https://mapbox.base.url"
                every { name() } returns "default"
                every { displayRef() } returns "RT 82"
                every { textColor() } returns "black"
            },
            legacyFallback = null,
        )
        val expected =
            "https://api.mapbox.com/styles/v1/userId/styleId/sprite.json?access_token=$ACCESS_TOKEN"

        val actual = toDownload.generateSpriteSheetUrl()

        assertEquals(expected, actual)
    }

    @Test
    fun `test mapbox legacy url generation`() {
        val toDownload = RouteShieldToDownload.MapboxLegacy(initialUrl = "https://mapbox.base.url")
        val expected = "https://mapbox.base.url.svg"

        val actual = toDownload.url

        assertEquals(expected, actual)
    }

    private companion object {
        const val ACCESS_TOKEN = "pk.123"
    }
}
