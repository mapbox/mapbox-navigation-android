package com.mapbox.navigation.tripdata.shield

import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShieldSpritesCacheTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val cache = ShieldSpritesCache()

    @Before
    fun setup() {
        mockkObject(RoadShieldDownloader)
    }

    @Test
    fun `download error`() = coroutineRule.runBlockingTest {
        val argument = "url"
        coEvery {
            RoadShieldDownloader.download(argument)
        } returns ExpectedFactory.createError("error")

        val result = cache.getOrRequest(argument)

        assertEquals(ResourceCache.RequestError("error", argument), result.error)
    }

    @Test
    fun `parsing error`() = coroutineRule.runBlockingTest {
        val argument = "url"
        coEvery {
            RoadShieldDownloader.download(argument)
        } returns ExpectedFactory.createValue("wrong json".toByteArray())

        val result = cache.getOrRequest(argument)

        assertTrue(
            result.error!!.error.startsWith("Error parsing shield sprites:"),
        )
    }

    @Test
    fun `download success`() = coroutineRule.runBlockingTest {
        val argument = "url"
        val expectedResult = ExpectedFactory.createValue<String, ByteArray>(
            """
                {
                  "turning-circle-outline": {
                    "width": 138,
                    "height": 138,
                    "x": 0,
                    "y": 0,
                    "pixelRatio": 1,
                    "visible": true
                  },
                  "turning-circle": {
                    "width": 126,
                    "height": 126,
                    "x": 138,
                    "y": 0,
                    "pixelRatio": 1,
                    "visible": true
                  }
                }
            """.trimIndent().toByteArray(),
        )
        coEvery {
            RoadShieldDownloader.download(argument)
        } coAnswers {
            delay(500L)
            expectedResult
        }

        val result = cache.getOrRequest(argument)

        assertEquals(
            ResourceCache.SuccessfulResponse(
                ShieldSprites.fromJson(String(expectedResult.value!!)),
                argument,
            ),
            result.value,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldDownloader)
    }
}
