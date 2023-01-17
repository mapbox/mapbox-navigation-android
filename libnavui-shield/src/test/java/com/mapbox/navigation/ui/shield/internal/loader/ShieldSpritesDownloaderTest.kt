package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.internal.RoadShieldDownloader
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShieldSpritesDownloaderTest {

    private lateinit var sut: ShieldSpritesDownloader

    @Before
    fun setup() {
        mockkObject(RoadShieldDownloader)
        sut = ShieldSpritesDownloader()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `download error`() = runBlockingTest {
        val argument = "url"
        coEvery {
            RoadShieldDownloader.download(argument)
        } returns ExpectedFactory.createError(Error("error"))

        val result = sut.load(argument)

        assertEquals("error", result.error?.message)
    }

    @Test
    fun `parsing error`() = runBlockingTest {
        val argument = "url"
        coEvery {
            RoadShieldDownloader.download(argument)
        } returns ExpectedFactory.createValue("wrong json".toByteArray())

        val result = sut.load(argument)

        assertTrue(
            result.error!!.message!!.startsWith("Error parsing shield sprites:")
        )
    }

    @Test
    fun `download success`() = runBlockingTest {
        val argument = "url"
        val expectedResult = ExpectedFactory.createValue<Error, ByteArray>(
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
            """.trimIndent().toByteArray()
        )
        coEvery {
            RoadShieldDownloader.download(argument)
        } coAnswers {
            delay(500L)
            expectedResult
        }

        val result = sut.load(argument)

        assertEquals(ShieldSprites.fromJson(String(expectedResult.value!!)), result.value)
    }
}
