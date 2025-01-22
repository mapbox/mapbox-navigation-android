package com.mapbox.navigation.tripdata.shield.model

import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.ShieldSpriteToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.generateSpriteSheetUrl
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteShieldToDownloadTest {

    @Test
    fun `test mapbox designed url generation`() {
        val toDownload = RouteShieldToDownload.MapboxDesign(
            shieldSpriteToDownload = ShieldSpriteToDownload("userId", "styleId"),
            accessToken = "pk.123",
            mapboxShield = mockk {
                every { baseUrl() } returns "https://mapbox.base.url"
                every { name() } returns "default"
                every { displayRef() } returns "RT 82"
                every { textColor() } returns "black"
            },
            legacyFallback = null,
        )
        val expected = "https://mapbox.base.url/userId/styleId/sprite/default-5?access_token=pk.123"

        val actual = toDownload.generateUrl(
            mockk { every { spriteName() } returns "default-5" },
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `test mapbox designed sprite sheet url generation`() {
        val toDownload = RouteShieldToDownload.MapboxDesign(
            shieldSpriteToDownload = ShieldSpriteToDownload("userId", "styleId"),
            accessToken = "pk.123",
            mapboxShield = mockk {
                every { baseUrl() } returns "https://mapbox.base.url"
                every { name() } returns "default"
                every { displayRef() } returns "RT 82"
                every { textColor() } returns "black"
            },
            legacyFallback = null,
        )
        val expected =
            "https://api.mapbox.com/styles/v1/userId/styleId/sprite.json?access_token=pk.123"

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
}
