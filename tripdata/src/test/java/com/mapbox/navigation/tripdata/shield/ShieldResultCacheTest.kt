package com.mapbox.navigation.tripdata.shield

import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.shield.api.ShieldFontConfig
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.ShieldSpriteToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.SizeSpecificSpriteInfo
import com.mapbox.navigation.tripdata.shield.internal.model.generateSpriteSheetUrl
import com.mapbox.navigation.tripdata.shield.internal.model.getSpriteFrom
import com.mapbox.navigation.tripdata.shield.model.RouteShield
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
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
        mockkStatic(ShieldSprites::toSizeSpecificSpriteInfos)
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
        val shieldSpriteInfos =
            listOf(mockk<SizeSpecificSpriteInfo>(), mockk<SizeSpecificSpriteInfo>())
        every { shieldSprites.toSizeSpecificSpriteInfos() } returns shieldSpriteInfos
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateUrl(shieldSprite) } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSpriteInfos) } returns shieldSprite
            every { shieldSpriteToDownload } returns ShieldSpriteToDownload(
                userId = "test-user",
                styleId = "test-style",
                fontConfig = null,
            )
            every { mapboxShield } returns mockk {
                every { textColor() } returns "black"
                every { displayRef() } returns "RT-82"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldSprites,
                spriteUrl,
            ),
        )
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                rawShieldJson.toByteArray(),
                shieldUrl,
            ),
        )

        val result = cache.getOrRequest(toDownload)

        assertTrue(result.error!!.error.startsWith("Error parsing shield svg:"))
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
        val shieldSpriteInfos =
            listOf(mockk<SizeSpecificSpriteInfo>(), mockk<SizeSpecificSpriteInfo>())
        every { shieldSprites.toSizeSpecificSpriteInfos() } returns shieldSpriteInfos
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateUrl(any()) } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSpriteInfos) } returns shieldSprite
            every { shieldSpriteToDownload } returns ShieldSpriteToDownload(
                userId = "test-user",
                styleId = "test-style",
                fontConfig = null,
            )
            every { mapboxShield } returns mockk {
                every { textColor() } returns "black"
                every { displayRef() } returns "RT-82"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldSprites,
                spriteUrl,
            ),
        )
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                rawShieldJson.toByteArray(),
                shieldUrl,
            ),
        )

        val expectedShieldString =
            """
                <svg xmlns="http://www.w3.org/2000/svg" id="default-5" width="114" height="42" viewBox="0 0 38 14"><g><path d="M0,0 H38 V14 H0 Z" fill="none"/><path d="M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="none" stroke="hsl(230, 18%, 13%)" stroke-linejoin="round" stroke-miterlimit="4px" stroke-width="2"/><path d="M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="hsl(0, 0%, 100%)"/><path d="M0,4 H38 V10 H0 Z" fill="none" id="mapbox-text-placeholder"/></g>	<text x="19.0" y="10.0" font-family="Arial, Helvetica, sans-serif" font-weight="bold" text-anchor="middle" font-size="9.0" fill="black">RT-82</text></svg>
            """.trimIndent()
        val expected = ResourceCache.SuccessfulResponse(
            RouteShield.MapboxDesignedShield(
                url = shieldUrl,
                byteArray = expectedShieldString.toByteArray(),
                mapboxShield = toDownload.mapboxShield,
                shieldSprite = shieldSprite,
            ),
            shieldUrl,
        )
        val result = cache.getOrRequest(toDownload)

        assertEquals(
            expectedShieldString,
            result.value?.response?.byteArray?.let { String(it) },
        )
        assertEquals(expected, result.value)
    }

    @Test
    fun `design shield - failure - shield download failure`() = coroutineRule.runBlockingTest {
        val shieldUrl = "shield-url"
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSpriteInfos =
            listOf(mockk<SizeSpecificSpriteInfo>(), mockk<SizeSpecificSpriteInfo>())
        every { shieldSprites.toSizeSpecificSpriteInfos() } returns shieldSpriteInfos
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateUrl(shieldSprite) } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSpriteInfos) } returns shieldSprite
            every { shieldSpriteToDownload } returns ShieldSpriteToDownload(
                userId = "test-user",
                styleId = "test-style",
                fontConfig = null,
            )
            every { mapboxShield } returns mockk {
                every { textColor() } returns "black"
                every { displayRef() } returns "RT-82"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldSprites,
                spriteUrl,
            ),
        )
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createError(ResourceCache.RequestError("error", shieldUrl))

        val expected = ResourceCache.RequestError("error", shieldUrl)
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
        } returns ExpectedFactory.createError(ResourceCache.RequestError("error", spriteUrl))

        val expected = ResourceCache.RequestError(
            """
                Error when downloading image sprite.
                url: $spriteUrl
                result: error
            """.trimIndent(),
            null,
        )
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    fun `design shield - failure - missing sprite`() = coroutineRule.runBlockingTest {
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSpriteInfos =
            listOf(mockk<SizeSpecificSpriteInfo>(), mockk<SizeSpecificSpriteInfo>())
        every { shieldSprites.toSizeSpecificSpriteInfos() } returns shieldSpriteInfos

        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSpriteInfos) } returns null
            every { shieldSpriteToDownload } returns ShieldSpriteToDownload(
                userId = "test-user",
                styleId = "test-style",
                fontConfig = null,
            )
            every { mapboxShield } returns mockk {
                every { name() } returns "name"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldSprites,
                spriteUrl,
            ),
        )

        val expected = ResourceCache.RequestError(
            "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites.",
            spriteUrl,
        )
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    fun `design shield - failure - missing placeholder`() = coroutineRule.runBlockingTest {
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSpriteInfos =
            listOf(mockk<SizeSpecificSpriteInfo>(), mockk<SizeSpecificSpriteInfo>())
        every { shieldSprites.toSizeSpecificSpriteInfos() } returns shieldSpriteInfos
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns null
            }
        }
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSpriteInfos) } returns shieldSprite
            every { shieldSpriteToDownload } returns ShieldSpriteToDownload(
                userId = "test-user",
                styleId = "test-style",
                fontConfig = null,
            )
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldSprites,
                spriteUrl,
            ),
        )

        val expected = ResourceCache.RequestError(
            "Mapbox shield sprite placeholder was null or empty in: $shieldSprite",
            spriteUrl,
        )
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
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldByteArray,
                shieldUrl.plus(".svg"),
            ),
        )

        val expected = ResourceCache.SuccessfulResponse(
            RouteShield.MapboxLegacyShield(
                url = toDownload.url,
                byteArray = shieldByteArray,
                initialUrl = shieldUrl,
            ),
            shieldUrl.plus(".svg"),
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
        } returns ExpectedFactory.createError(ResourceCache.RequestError("error", shieldUrl))

        val expected = ResourceCache.RequestError("error", shieldUrl)
        val result = cache.getOrRequest(toDownload)

        assertEquals(expected, result.error)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `design shield - custom font success`() = coroutineRule.runBlockingTest {
        val rawShieldJson =
            """
                {"svg":"<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"default-5\" width=\"114\" height=\"42\" viewBox=\"0 0 38 14\"><g><path d=\"M0,0 H38 V14 H0 Z\" fill=\"none\"/><path d=\"M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1\" fill=\"none\" stroke=\"hsl(230, 18%, 13%)\" stroke-linejoin=\"round\" stroke-miterlimit=\"4px\" stroke-width=\"2\"/><path d=\"M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1\" fill=\"hsl(0, 0%, 100%)\"/><path d=\"M0,4 H38 V10 H0 Z\" fill=\"none\" id=\"mapbox-text-placeholder\"/></g></svg>"}
            """.trimIndent()

        val shieldUrl = "shield-url"
        val spriteUrl = "sprite-url"
        val shieldSprites = mockk<ShieldSprites>()
        val shieldSpriteInfos =
            listOf(mockk<SizeSpecificSpriteInfo>(), mockk<SizeSpecificSpriteInfo>())
        every { shieldSprites.toSizeSpecificSpriteInfos() } returns shieldSpriteInfos
        val shieldSprite = mockk<ShieldSprite> {
            every { spriteAttributes() } returns mockk {
                every { placeholder() } returns listOf(10.0, 4.0, 18.0, 10.0)
            }
        }
        val customFontConfig = ShieldFontConfig.Builder("CustomFont")
            .fontWeight(ShieldFontConfig.FontWeight.BOLD)
            .fontStyle(ShieldFontConfig.FontStyle.ITALIC)
            .build()
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateUrl(any()) } returns shieldUrl
            every { generateSpriteSheetUrl() } returns spriteUrl
            every { getSpriteFrom(shieldSpriteInfos) } returns shieldSprite
            every { shieldSpriteToDownload } returns ShieldSpriteToDownload(
                userId = "test-user",
                styleId = "test-style",
                fontConfig = customFontConfig,
            )
            every { mapboxShield } returns mockk {
                every { textColor() } returns "red"
                every { displayRef() } returns "C-123"
            }
        }
        coEvery {
            shieldSpritesCache.getOrRequest(spriteUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                shieldSprites,
                spriteUrl,
            ),
        )
        coEvery {
            shieldByteArrayCache.getOrRequest(shieldUrl)
        } returns ExpectedFactory.createValue(
            ResourceCache.SuccessfulResponse(
                rawShieldJson.toByteArray(),
                shieldUrl,
            ),
        )

        val expectedShieldString =
            """
                <svg xmlns="http://www.w3.org/2000/svg" id="default-5" width="114" height="42" viewBox="0 0 38 14"><g><path d="M0,0 H38 V14 H0 Z" fill="none"/><path d="M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="none" stroke="hsl(230, 18%, 13%)" stroke-linejoin="round" stroke-miterlimit="4px" stroke-width="2"/><path d="M3,1 H35 C35,1 37,1 37,3 V11 C37,11 37,13 35,13 H3 C3,13 1,13 1,11 V3 C1,3 1,1 3,1" fill="hsl(0, 0%, 100%)"/><path d="M0,4 H38 V10 H0 Z" fill="none" id="mapbox-text-placeholder"/></g>	<text x="19.0" y="10.0" font-family="CustomFont" font-weight="700" font-style="italic" text-anchor="middle" font-size="9.0" fill="red">C-123</text></svg>
            """.trimIndent()
        val expected = ResourceCache.SuccessfulResponse(
            RouteShield.MapboxDesignedShield(
                url = shieldUrl,
                byteArray = expectedShieldString.toByteArray(),
                mapboxShield = toDownload.mapboxShield,
                shieldSprite = shieldSprite,
            ),
            shieldUrl,
        )
        val result = cache.getOrRequest(toDownload)

        assertEquals(
            expectedShieldString,
            result.value?.response?.byteArray?.let { String(it) },
        )
        assertEquals(expected, result.value)
    }

    @After
    fun tearDown() {
        unmockkStatic(RouteShieldToDownload.MapboxDesign::generateSpriteSheetUrl)
        unmockkStatic(RouteShieldToDownload.MapboxDesign::getSpriteFrom)
        unmockkStatic(ShieldSprites::toSizeSpecificSpriteInfos)
    }
}
