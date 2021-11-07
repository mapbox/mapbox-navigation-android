package com.mapbox.navigation.ui.shield.internal.model

import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            legacyFallback = null
        )
        val expected = "https://mapbox.base.url/userId/styleId/sprite/default-5?access_token=pk.123"

        val actual = toDownload.url

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
            legacyFallback = null
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

    @Test
    fun `test mapbox designed sprite available`() {
        val mockSprite1 = mockk<ShieldSprite> {
            every { spriteName() } returns "us-interstate-3"
            every { spriteAttributes() } returns mockk {
                every { width() } returns 72
                every { height() } returns 24
                every { x() } returns 12
                every { y() } returns 10
                every { pixelRatio() } returns 1
                every { placeholder() } returns listOf(0.0, 4.0, 72.0, 20.0)
                every { visible() } returns true
            }
        }
        val mockSprite2 = mockk<ShieldSprite> {
            every { spriteName() } returns "default-5"
            every { spriteAttributes() } returns mockk {
                every { width() } returns 66
                every { height() } returns 22
                every { x() } returns 8
                every { y() } returns 6
                every { pixelRatio() } returns 2
                every { placeholder() } returns listOf(0.0, 4.0, 60.0, 18.0)
                every { visible() } returns true
            }
        }
        val shieldSprites = ShieldSprites
            .builder()
            .sprites(listOf(mockSprite1, mockSprite2))
            .build()
        val toDownload = RouteShieldToDownload.MapboxDesign(
            shieldSpriteToDownload = ShieldSpriteToDownload("userId", "styleId"),
            accessToken = "pk.123",
            mapboxShield = mockk {
                every { baseUrl() } returns "https://mapbox.base.url"
                every { name() } returns "default"
                every { displayRef() } returns "RT 82"
                every { textColor() } returns "black"
            },
            legacyFallback = null
        )

        val actual = toDownload.getSpriteFrom(shieldSprites)

        assertEquals(mockSprite2, actual)
    }

    @Test
    fun `test mapbox designed sprite unavailable`() {
        val mockSprite = mockk<ShieldSprite> {
            every { spriteName() } returns "us-interstate-3"
            every { spriteAttributes() } returns mockk {
                every { width() } returns 72
                every { height() } returns 24
                every { x() } returns 12
                every { y() } returns 10
                every { pixelRatio() } returns 1
                every { placeholder() } returns listOf(0.0, 4.0, 72.0, 20.0)
                every { visible() } returns true
            }
        }
        val shieldSprites = ShieldSprites
            .builder()
            .sprites(listOf(mockSprite))
            .build()
        val toDownload = RouteShieldToDownload.MapboxDesign(
            shieldSpriteToDownload = ShieldSpriteToDownload("userId", "styleId"),
            accessToken = "pk.123",
            mapboxShield = mockk {
                every { baseUrl() } returns "https://mapbox.base.url"
                every { name() } returns "default"
                every { displayRef() } returns "RT 82"
                every { textColor() } returns "black"
            },
            legacyFallback = null
        )

        val actual = toDownload.getSpriteFrom(shieldSprites)

        assertNull(actual)
    }
}
