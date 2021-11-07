package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldOrigin
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoadShieldContentManagerImplTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `request waits for all results to be available (success and manual cancellation), async`() =
        coroutineRule.runBlockingTest {
            val cache = mockk<ShieldResultCache>()
            val contentManager = createContentManager(cache)

            val legacyShieldUrl = "url_legacy"
            val toDownloadLegacy = mockk<RouteShieldToDownload.MapboxLegacy> {
                every { url } returns legacyShieldUrl
            }
            val expectedLegacyShield = RouteShield.MapboxLegacyShield(
                legacyShieldUrl,
                byteArrayOf()
            )
            val expectedLegacyResult = RouteShieldResult(
                expectedLegacyShield,
                RouteShieldOrigin(
                    isFallback = false,
                    originalUrl = legacyShieldUrl,
                    originalErrorMessage = ""
                )
            )
            coEvery {
                cache.getOrRequest(toDownloadLegacy)
            } coAnswers {
                delay(500L)
                ExpectedFactory.createValue(expectedLegacyShield)
            }

            val designShieldUrl = "url"
            val toDownloadDesign = mockk<RouteShieldToDownload.MapboxDesign> {
                every { url } returns designShieldUrl
                every { legacyFallback } returns null
            }
            val expectedDesignResult = RouteShieldError(
                designShieldUrl,
                RoadShieldContentManagerImpl.CANCELED_MESSAGE
            )
            coEvery {
                cache.getOrRequest(toDownloadDesign)
            } coAnswers {
                try {
                    delay(1000L)
                    ExpectedFactory.createError("error")
                } catch (ex: CancellationException) {
                    ExpectedFactory.createError(RoadShieldContentManagerImpl.CANCELED_MESSAGE)
                }
            }

            var result: List<Expected<RouteShieldError, RouteShieldResult>>? = null
            pauseDispatcher {
                launch {
                    result = contentManager.getShields(listOf(toDownloadLegacy, toDownloadDesign))
                }
                // run coroutines until they hit the download delays
                runCurrent()
                // advance by enough to only download one shield
                advanceTimeBy(600L)
                contentManager.cancelAll()
            }
            assertEquals(expectedLegacyResult, result!![0].value)
            assertEquals(expectedDesignResult, result!![1].error)
        }

    @Test
    fun `request waits for all results to be available (success and job cancellation), async`() =
        coroutineRule.runBlockingTest {
            val cache = mockk<ShieldResultCache>()
            val contentManager = createContentManager(cache)

            val legacyShieldUrl = "url_legacy"
            val toDownloadLegacy = mockk<RouteShieldToDownload.MapboxLegacy> {
                every { url } returns legacyShieldUrl
            }
            val expectedLegacyShield = RouteShield.MapboxLegacyShield(
                legacyShieldUrl,
                byteArrayOf()
            )
            val expectedLegacyResult = RouteShieldResult(
                expectedLegacyShield,
                RouteShieldOrigin(
                    isFallback = false,
                    originalUrl = legacyShieldUrl,
                    originalErrorMessage = ""
                )
            )
            coEvery {
                cache.getOrRequest(toDownloadLegacy)
            } coAnswers {
                delay(500L)
                ExpectedFactory.createValue(expectedLegacyShield)
            }

            val designShieldUrl = "url"
            val toDownloadDesign = mockk<RouteShieldToDownload.MapboxDesign> {
                every { url } returns designShieldUrl
                every { legacyFallback } returns null
            }
            val expectedDesignResult = RouteShieldError(
                designShieldUrl,
                RoadShieldContentManagerImpl.CANCELED_MESSAGE
            )
            coEvery {
                cache.getOrRequest(toDownloadDesign)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createError("error")
            }

            var result: List<Expected<RouteShieldError, RouteShieldResult>>? = null
            pauseDispatcher {
                val job = launch {
                    result = contentManager.getShields(listOf(toDownloadLegacy, toDownloadDesign))
                }
                // run coroutines until they hit the download delays
                runCurrent()
                // advance by enough to only download one shield
                advanceTimeBy(600L)
                job.cancel()
                job.join()
            }
            assertEquals(expectedLegacyResult, result!![0].value)
            assertEquals(expectedDesignResult, result!![1].error)
        }

    @Test
    fun `request waits for all results to be available (success and failure), async`() =
        coroutineRule.runBlockingTest {
            val cache = mockk<ShieldResultCache>()
            val contentManager = createContentManager(cache)

            val legacyShieldUrl = "url_legacy"
            val toDownloadLegacy = mockk<RouteShieldToDownload.MapboxLegacy> {
                every { url } returns legacyShieldUrl
            }
            val expectedLegacyShield = RouteShield.MapboxLegacyShield(
                legacyShieldUrl,
                byteArrayOf()
            )
            val expectedLegacyResult = RouteShieldResult(
                expectedLegacyShield,
                RouteShieldOrigin(
                    isFallback = false,
                    originalUrl = legacyShieldUrl,
                    originalErrorMessage = ""
                )
            )
            coEvery {
                cache.getOrRequest(toDownloadLegacy)
            } coAnswers {
                delay(1000L)
                ExpectedFactory.createValue(expectedLegacyShield)
            }

            val designShieldUrl = "url"
            val toDownloadDesign = mockk<RouteShieldToDownload.MapboxDesign> {
                every { url } returns designShieldUrl
                every { legacyFallback } returns null
            }
            val expectedDesignResult = RouteShieldError(
                designShieldUrl,
                "error"
            )
            coEvery {
                cache.getOrRequest(toDownloadDesign)
            } coAnswers {
                delay(500L)
                ExpectedFactory.createError("error")
            }

            var result: List<Expected<RouteShieldError, RouteShieldResult>>? = null
            pauseDispatcher {
                result = contentManager.getShields(listOf(toDownloadLegacy, toDownloadDesign))
            }
            assertEquals(expectedLegacyResult, result!![0].value)
            assertEquals(expectedDesignResult, result!![1].error)
        }

    @Test
    fun `request waits for all results to be available (success and failure), sync`() =
        coroutineRule.runBlockingTest {
            val cache = mockk<ShieldResultCache>()
            val contentManager = createContentManager(cache)

            val legacyShieldUrl = "url_legacy"
            val toDownloadLegacy = mockk<RouteShieldToDownload.MapboxLegacy> {
                every { url } returns legacyShieldUrl
            }
            val expectedLegacyShield = RouteShield.MapboxLegacyShield(
                legacyShieldUrl,
                byteArrayOf()
            )
            val expectedLegacyResult = RouteShieldResult(
                expectedLegacyShield,
                RouteShieldOrigin(
                    isFallback = false,
                    originalUrl = legacyShieldUrl,
                    originalErrorMessage = ""
                )
            )
            coEvery {
                cache.getOrRequest(toDownloadLegacy)
            } returns ExpectedFactory.createValue(expectedLegacyShield)

            val designShieldUrl = "url"
            val toDownloadDesign = mockk<RouteShieldToDownload.MapboxDesign> {
                every { url } returns designShieldUrl
                every { legacyFallback } returns null
            }
            val expectedDesignResult = RouteShieldError(
                designShieldUrl,
                "error"
            )
            coEvery {
                cache.getOrRequest(toDownloadDesign)
            } returns ExpectedFactory.createError("error")

            val result = contentManager.getShields(listOf(toDownloadLegacy, toDownloadDesign))

            assertEquals(expectedLegacyResult, result[0].value)
            assertEquals(expectedDesignResult, result[1].error)
        }

    @Test
    fun `unsuccessful design shield results with successful fallback`() =
        coroutineRule.runBlockingTest {
            val cache = mockk<ShieldResultCache>()
            val contentManager = createContentManager(cache)

            val shieldUrl = "url"
            val legacyShieldUrl = "legacy_url"
            val expectedLegacyShield = RouteShield.MapboxLegacyShield(
                shieldUrl,
                byteArrayOf()
            )
            val expectedResult = RouteShieldResult(
                expectedLegacyShield,
                RouteShieldOrigin(
                    isFallback = true,
                    originalUrl = shieldUrl,
                    originalErrorMessage = "error"
                )
            )
            val legacyToDownload = RouteShieldToDownload.MapboxLegacy(legacyShieldUrl)
            val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
                every { url } returns shieldUrl
                every { legacyFallback } returns legacyToDownload
            }

            coEvery {
                cache.getOrRequest(toDownload)
            } returns ExpectedFactory.createError("error")
            coEvery {
                cache.getOrRequest(legacyToDownload)
            } returns ExpectedFactory.createValue(expectedLegacyShield)

            val result = contentManager.getShields(listOf(toDownload))

            assertEquals(expectedResult, result.first().value)
        }

    @Test
    fun `unsuccessful design shield results with unsuccessful fallback`() =
        coroutineRule.runBlockingTest {
            val cache = mockk<ShieldResultCache>()
            val contentManager = createContentManager(cache)

            val shieldUrl = "url"
            val legacyShieldUrl = "legacy_url"
            val expectedResult = RouteShieldError(
                shieldUrl,
                """
                    |original request failed with:
                    |url: url
                    |error: error
                    |
                    |fallback request failed with:
                    |url: legacy_url.svg
                    |error: error_legacy
                """.trimMargin()
            )
            val legacyToDownload = RouteShieldToDownload.MapboxLegacy(legacyShieldUrl)
            val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
                every { url } returns shieldUrl
                every { legacyFallback } returns legacyToDownload
            }

            coEvery {
                cache.getOrRequest(toDownload)
            } returns ExpectedFactory.createError("error")
            coEvery {
                cache.getOrRequest(legacyToDownload)
            } returns ExpectedFactory.createError("error_legacy")

            val result = contentManager.getShields(listOf(toDownload))

            assertEquals(expectedResult, result.first().error)
        }

    @Test
    fun `unsuccessful design shield results without fallback`() = coroutineRule.runBlockingTest {
        val cache = mockk<ShieldResultCache>()
        val contentManager = createContentManager(cache)

        val shieldUrl = "url"
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { url } returns shieldUrl
            every { legacyFallback } returns null
        }
        val expectedResult = RouteShieldError(
            shieldUrl,
            "error"
        )

        coEvery {
            cache.getOrRequest(toDownload)
        } returns ExpectedFactory.createError("error")

        val result = contentManager.getShields(listOf(toDownload))

        assertEquals(expectedResult, result.first().error)
    }

    @Test
    fun `successful design shield results`() = coroutineRule.runBlockingTest {
        val cache = mockk<ShieldResultCache>()
        val contentManager = createContentManager(cache)

        val shieldUrl = "url"
        val mapboxShield = mockk<MapboxShield>()
        val sprite = mockk<ShieldSprite>()
        val toDownload = mockk<RouteShieldToDownload.MapboxDesign> {
            every { url } returns shieldUrl
        }
        val expectedShield = RouteShield.MapboxDesignedShield(
            shieldUrl,
            byteArrayOf(),
            mapboxShield,
            sprite
        )
        val expectedResult = RouteShieldResult(
            expectedShield,
            RouteShieldOrigin(
                isFallback = false,
                originalUrl = shieldUrl,
                originalErrorMessage = ""
            )
        )

        coEvery {
            cache.getOrRequest(toDownload)
        } returns ExpectedFactory.createValue(expectedShield)

        val result = contentManager.getShields(listOf(toDownload))

        assertEquals(expectedResult, result.first().value)
    }

    @Test
    fun `successful legacy shield results`() = coroutineRule.runBlockingTest {
        val cache = mockk<ShieldResultCache>()
        val contentManager = createContentManager(cache)

        val shieldUrl = "url"
        val toDownload = RouteShieldToDownload.MapboxLegacy(shieldUrl)
        val expectedShield = RouteShield.MapboxLegacyShield(
            shieldUrl,
            byteArrayOf()
        )
        val expectedResult = RouteShieldResult(
            expectedShield,
            RouteShieldOrigin(
                isFallback = false,
                originalUrl = toDownload.url,
                originalErrorMessage = ""
            )
        )

        coEvery {
            cache.getOrRequest(toDownload)
        } returns ExpectedFactory.createValue(expectedShield)

        val result = contentManager.getShields(listOf(toDownload))

        assertEquals(expectedResult, result.first().value)
    }

    @Test
    fun `unsuccessful legacy shield results`() = coroutineRule.runBlockingTest {
        val cache = mockk<ShieldResultCache>()
        val contentManager = createContentManager(cache)

        val shieldUrl = "url"
        val toDownload = RouteShieldToDownload.MapboxLegacy(shieldUrl)
        val expectedResult = RouteShieldError(
            toDownload.url,
            "error"
        )

        coEvery {
            cache.getOrRequest(toDownload)
        } returns ExpectedFactory.createError("error")

        val result = contentManager.getShields(listOf(toDownload))

        assertEquals(expectedResult, result.first().error)
    }

    private fun createContentManager(
        shieldResultCache: ShieldResultCache = mockk()
    ): RoadShieldContentManagerImpl = RoadShieldContentManagerImpl(shieldResultCache)
}
