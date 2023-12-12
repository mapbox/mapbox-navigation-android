package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.internal.model.generateSpriteSheetUrl
import com.mapbox.navigation.ui.shield.internal.model.getSpriteFrom
import com.mapbox.navigation.ui.shield.model.RouteShield
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShieldResultCacheTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val shieldSpritesCache: ShieldSpritesCache = mockk()
    private val shieldByteArrayCache: ShieldByteArrayCache = mockk()

    private val cache = ShieldResultCache(shieldSpritesCache, shieldByteArrayCache)

    @Before
    fun setup() {
        mockkStatic(RouteShieldToDownload.MapboxDesign::generateSpriteSheetUrl)
        mockkStatic(RouteShieldToDownload.MapboxDesign::getSpriteFrom)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `design shield - success`() = coroutineRule.runBlockingTest {
        val rawShieldJson =
            """
                {"svg":"<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"default-5\" width=\"114\" height=\"42\" viewBox=\"0 0 38 14\"><g><path d=\"M0,0 H38 V14 H0 Z\" fill=\"none\"/><path d=\"M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1\" fill=\"none\" stroke=\"hsl(230, 18%, 13%)\" stroke-linejoin=\"round\" stroke-miterlimit=\"4px\" stroke-width=\"2\"/><path d=\"M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1\" fill=\"hsl(0, 0%, 100%)\"/><path d=\"M0,4 H38 V10 H0 Z\" fill=\"none\" id=\"mapbox-text-placeholder\"/></g></svg>"}
            """.trimIndent()

        val shieldUrl = "shield-url"
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { url } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSprites) } returns shieldSprite
            every { mapboxShield } returns mockk {
                every { textColor() } returns "black"
                every { displayRef() } returns "RT-82"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createValue(rawShieldJson.toByteArray())

        val expectedShieldString =
            """
                <svg xmlns="http://www.w3.org/2000/svg" id="default-5" width="114" height="42" viewBox="0 0 38 14"><g><path d="M0,0 H38 V14 H0 Z" fill="none"/><path d="M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="none" stroke="hsl(230, 18%, 13%)" stroke-linejoin="round" stroke-miterlimit="4px" stroke-width="2"/><path d="M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="hsl(0, 0%, 100%)"/><path d="M0,4 H38 V10 H0 Z" fill="none" id="mapbox-text-placeholder"/></g>	<text x="19.0" y="10.0" font-family="Arial, Helvetica, sans-serif" font-weight="bold" text-anchor="middle" font-size="9.0" fill="black">RT-82</text></svg>
            """.trimIndent()
        val expected = RouteShield.MapboxDesignedShield(
            url = toDownload.url,
            byteArray = expectedShieldString.toByteArray(),
            mapboxShield = toDownload.mapboxShield,
            shieldSprite = shieldSprite
        )
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.value)
    }

    @Test
    fun `design shield - parsing failure`() = coroutineRule.runBlockingTest {
        val rawShieldJson =
            """
                {"svg":{"aa":"bb"}}
            """.trimIndent()

        val shieldUrl = "shield-url"
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { url } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSprites) } returns shieldSprite
            every { mapboxShield } returns mockk {
                every { textColor() } returns "black"
                every { displayRef() } returns "RT-82"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createValue(rawShieldJson.toByteArray())

        val result = cache.getOrRequest(toDownload)

        assertTrue(result.error!!.startsWith("Error parsing shield svg:"))
    }

    @Test
    fun `design shield - failure - shield download failure`() = coroutineRule.runBlockingTest {
        val shieldUrl = "shield-url"
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { url } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSprites) } returns shieldSprite
            every { mapboxShield } returns mockk {
                every { textColor() } returns "black"
                every { displayRef() } returns "RT-82"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createError("error")

        val expected = "error"
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    fun `design shield - failure - missing sprites`() = coroutineRule.runBlockingTest {
        val spriteUrl = "sprite-url"
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateSpriteSheetUrl() } returns spriteUrl
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createError("error")

        val expected = """
            Error when downloading image sprite.
            url: $spriteUrl
            result: error
        """.trimIndent()
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    fun `design shield - failure - missing sprite`() = coroutineRule.runBlockingTest {
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSprites) } returns null
            every { mapboxShield } returns mockk {
                every { name() } returns "name"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)

        val expected = "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites."
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    fun `design shield - failure - missing placeholder`() = coroutineRule.runBlockingTest {
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns null
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSprites) } returns shieldSprite
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)

        val expected = "Mapbox shield sprite placeholder was null or empty in: $shieldSprite"
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    fun `legacy shield - success`() = coroutineRule.runBlockingTest {
        val shieldByteArray = byteArrayOf()
        val shieldUrl = "shield-url"
        val toDownload = mockk<RouteShieldToDownload.MapboxLegacy> {
            every { initialUrl } returns shieldUrl
            every { url } returns shieldUrl.plus(".svg")
        }
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl.plus(".svg"))
        } returns ExpectedFactory.createValue(shieldByteArray)

        val expected = RouteShield.MapboxLegacyShield(
            url = toDownload.url,
            byteArray = shieldByteArray,
            initialUrl = shieldUrl
        )
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.value)
    }

    @Test
    fun `legacy shield - failure`() = coroutineRule.runBlockingTest {
        val shieldUrl = "shield-url"
        val toDownload = mockk<RouteShieldToDownload.MapboxLegacy> {
            every { url } returns shieldUrl
        }
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createError("error")

        val expected = "error"
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @After
    fun tearDown() {
        unmockkStatic(RouteShieldToDownload.MapboxDesign::generateSpriteSheetUrl)
        unmockkStatic(RouteShieldToDownload.MapboxDesign::getSpriteFrom)
    }
}
