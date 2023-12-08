package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.internal.model.generateSpriteSheetUrl
import com.mapbox.navigation.ui.shield.internal.model.getSpriteFrom
import com.mapbox.navigation.ui.shield.model.RouteShield
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoadShieldLoaderTest {

    private lateinit var sut: RoadShieldLoader

    private lateinit var spritesLoader: Loader<String, ShieldSprites>
    private lateinit var imageLoader: Loader<String, ByteArray>

    @Before
    fun setup() {
        spritesLoader = mockk()
        imageLoader = mockk()
        sut = RoadShieldLoader(spritesLoader, imageLoader)

        mockkStatic(RouteShieldToDownload.MapboxDesign::generateSpriteSheetUrl)
        mockkStatic(RouteShieldToDownload.MapboxDesign::getSpriteFrom)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `design shield - success`() = runBlockingTest {
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
            spritesLoader.load(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)
        coEvery {
            imageLoader.load(shieldUrl)
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
        val result = sut.load(toDownload)

        assertEquals(expected, result.value)
    }

    @Test
    fun `design shield - failure - shield download failure`() = runBlockingTest {
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
            spritesLoader.load(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)
        coEvery {
            imageLoader.load(shieldUrl)
        } returns ExpectedFactory.createError(Error("error"))

        val expected = "error"
        val result = sut.load(toDownload)

        assertEquals(expected, result.error?.message)
    }

    @Test
    fun `design shield - failure - missing sprites`() = runBlockingTest {
        val spriteUrl = "sprite-url"
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { generateSpriteSheetUrl() } returns spriteUrl
        }
        coEvery {
            spritesLoader.load(spriteUrl)
        } returns ExpectedFactory.createError(Error("error"))

        val expected = """
            Error when downloading image sprite.
            url: $spriteUrl
            result: error
        """.trimIndent()
        val result = sut.load(toDownload)

        assertEquals(expected, result.error?.message)
    }

    @Test
    fun `design shield - failure - missing sprite`() = runBlockingTest {
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
            spritesLoader.load(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)

        val expected = "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites."
        val result = sut.load(toDownload)

        assertEquals(expected, result.error?.message)
    }

    @Test
    fun `design shield - failure - missing placeholder`() = runBlockingTest {
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
            spritesLoader.load(spriteUrl)
        } returns ExpectedFactory.createValue(shieldSprites)

        val expected = "Mapbox shield sprite placeholder was null or empty in: $shieldSprite"
        val result = sut.load(toDownload)

        assertEquals(expected, result.error?.message)
    }

    @Test
    fun `legacy shield - success`() = runBlockingTest {
        val shieldByteArray = byteArrayOf()
        val shieldUrl = "shield-url"
        val toDownload = mockk<RouteShieldToDownload.MapboxLegacy> {
            every { initialUrl } returns shieldUrl
            every { url } returns shieldUrl.plus(".svg")
        }
        coEvery {
            imageLoader.load(shieldUrl.plus(".svg"))
        } returns ExpectedFactory.createValue(shieldByteArray)

        val expected = RouteShield.MapboxLegacyShield(
            url = toDownload.url,
            byteArray = shieldByteArray,
            initialUrl = shieldUrl
        )
        val result = sut.load(toDownload)

        assertEquals(expected, result.value)
    }

    @Test
    fun `legacy shield - failure`() = runBlockingTest {
        val shieldUrl = "shield-url"
        val toDownload = mockk<RouteShieldToDownload.MapboxLegacy> {
            every { url } returns shieldUrl
        }
        coEvery {
            imageLoader.load(shieldUrl)
        } returns ExpectedFactory.createError(Error("error"))

        val expected = "error"
        val result = sut.load(toDownload)

        assertEquals(expected, result.error?.message)
    }
}
