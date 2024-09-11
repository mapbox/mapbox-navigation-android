package com.mapbox.navigation.tripdata.shield

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.pauseDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private typealias DownloadResult = Expected<String, ByteArray>
private typealias CacheResult =
    Expected<ResourceCache.RequestError, ResourceCache.SuccessfulResponse<ByteArray>>

private const val RequestDelay = 500L

/**
 * This tests the entire logic of [ResourceCache] since [ShieldByteArrayCache] is the simplest implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShieldByteArrayCacheTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val cache = ShieldByteArrayCache()
    private val downloadValueResult = ExpectedFactory.createValue<String, ByteArray>(byteArrayOf())
    private val downloadErrorResult = ExpectedFactory.createError<String, ByteArray>("error")
    private val downloadValueAsyncAnswer:
        suspend MockKAnswerScope<DownloadResult, DownloadResult>.(Call) -> DownloadResult = {
            delay(RequestDelay)
            downloadValueResult
        }
    private val downloadErrorAsyncAnswer:
        suspend MockKAnswerScope<DownloadResult, DownloadResult>.(Call) -> DownloadResult = {
            delay(RequestDelay)
            downloadErrorResult
        }

    @Before
    fun setup() {
        mockkObject(RoadShieldDownloader)
    }

    @Test
    fun `single request returns success`() = coroutineRule.runBlockingTest {
        val argument = "url"
        coEvery { RoadShieldDownloader.download(argument) } returns downloadValueResult

        val result = cache.getOrRequest(argument)

        assertEquals(
            ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
            result.value,
        )
    }

    @Test
    fun `single request returns failure`() = coroutineRule.runBlockingTest {
        val argument = "url"
        coEvery { RoadShieldDownloader.download(argument) } returns downloadErrorResult

        val result = cache.getOrRequest(argument)

        assertEquals(
            ResourceCache.RequestError(downloadErrorResult.error!!, argument),
            result.error,
        )
    }

    @Test
    fun `cache hit for previous success on synchronous attempt`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } returns downloadValueResult

            val firstResult = cache.getOrRequest(argument)
            val secondResult = cache.getOrRequest(argument)

            coVerify(exactly = 1) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                firstResult.value,
            )
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                secondResult.value,
            )
        }

    @Test
    fun `retry for previous failure on synchronous attempt, still fails`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } returns downloadErrorResult

            val firstResult = cache.getOrRequest(argument)
            val secondResult = cache.getOrRequest(argument)

            // all coroutines are run synchronously, so the second one is not suspending
            coVerify(exactly = 2) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.RequestError(downloadErrorResult.error!!, argument),
                firstResult.error,
            )
            assertEquals(
                ResourceCache.RequestError(downloadErrorResult.error!!, argument),
                secondResult.error,
            )
        }

    @Test
    fun `retry for previous failure on synchronous attempt, succeeds`() =
        coroutineRule.runBlockingTest {
            val argument = "url"

            coEvery { RoadShieldDownloader.download(argument) } returns downloadErrorResult
            val firstResult = cache.getOrRequest(argument)
            coEvery { RoadShieldDownloader.download(argument) } returns downloadValueResult
            val secondResult = cache.getOrRequest(argument)

            // all coroutines are run synchronously, so the second one is not suspending
            coVerify(exactly = 2) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.RequestError(downloadErrorResult.error!!, argument),
                firstResult.error,
            )
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                secondResult.value,
            )
        }

    @Test
    fun `duplicate async request awaits results for success`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } coAnswers downloadValueAsyncAnswer

            var firstResult: CacheResult? = null
            var secondResult: CacheResult? = null
            pauseDispatcher {
                launch {
                    firstResult = cache.getOrRequest(argument)
                }
                launch {
                    secondResult = cache.getOrRequest(argument)
                }
            }

            coVerify(exactly = 1) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                firstResult!!.value,
            )
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                secondResult!!.value,
            )
        }

    @Test
    fun `duplicate async request awaits results for failure`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } coAnswers downloadErrorAsyncAnswer

            var firstResult: CacheResult? = null
            var secondResult: CacheResult? = null
            pauseDispatcher {
                launch {
                    firstResult = cache.getOrRequest(argument)
                }
                launch {
                    secondResult = cache.getOrRequest(argument)
                }
            }

            coVerify(exactly = 1) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.RequestError(downloadErrorResult.error!!, argument),
                firstResult!!.error,
            )
            assertEquals(
                ResourceCache.RequestError(downloadErrorResult.error!!, argument),
                secondResult!!.error,
            )
        }

    @Test
    fun `requests for different resources are executed in parallel`() =
        coroutineRule.runBlockingTest {
            val argument1 = "url1"
            val argument2 = "url2"
            val expectedResult1 = ExpectedFactory.createValue<String, ByteArray>(byteArrayOf(0, 1))
            val expectedResult2 = ExpectedFactory.createValue<String, ByteArray>(byteArrayOf(2, 3))
            coEvery { RoadShieldDownloader.download(argument1) } coAnswers {
                delay(1000L)
                expectedResult1
            }
            coEvery { RoadShieldDownloader.download(argument2) } coAnswers {
                delay(500L)
                expectedResult2
            }

            var firstResult: CacheResult? = null
            var secondResult: CacheResult? = null
            pauseDispatcher {
                launch {
                    firstResult = cache.getOrRequest(argument1)
                }
                launch {
                    secondResult = cache.getOrRequest(argument2)
                }

                // advance to let second get downloaded but not the first
                advanceTimeBy(750L)

                // verify that indeed first was scheduled to be downloaded earlier than second
                coVerifyOrder {
                    RoadShieldDownloader.download(argument1)
                    RoadShieldDownloader.download(argument2)
                }

                // second (shorter) request should already finish while first (longer) is ongoing
                assertEquals(
                    ResourceCache.SuccessfulResponse(expectedResult2.value, argument2),
                    secondResult!!.value,
                )
                assertNull(firstResult)
            }

            // both requests eventually finish
            assertEquals(
                ResourceCache.SuccessfulResponse(expectedResult1.value, argument1),
                firstResult!!.value,
            )
            assertEquals(
                ResourceCache.SuccessfulResponse(expectedResult2.value, argument2),
                secondResult!!.value,
            )
        }

    @Test
    fun `non-overlapping duplicate async request hit cache for success`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } coAnswers downloadValueAsyncAnswer

            var firstResult: CacheResult? = null
            var secondResult: CacheResult? = null
            pauseDispatcher {
                launch {
                    firstResult = cache.getOrRequest(argument)
                }
                // let first request finish
                advanceTimeBy(RequestDelay + 100)
                launch {
                    secondResult = cache.getOrRequest(argument)
                }
            }

            coVerify(exactly = 1) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                firstResult!!.value,
            )
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                secondResult!!.value,
            )
        }

    @Test
    fun `non-overlapping duplicate async request retry for failure`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } coAnswers downloadErrorAsyncAnswer

            var firstResult: CacheResult? = null
            var secondResult: CacheResult? = null
            pauseDispatcher {
                launch {
                    firstResult = cache.getOrRequest(argument)
                }
                // let first request finish
                advanceTimeBy(RequestDelay + 100)
                coEvery {
                    RoadShieldDownloader.download(argument)
                } coAnswers downloadValueAsyncAnswer
                launch {
                    secondResult = cache.getOrRequest(argument)
                }
            }

            coVerify(exactly = 2) {
                RoadShieldDownloader.download(argument)
            }
            assertEquals(
                ResourceCache.RequestError(downloadErrorResult.error!!, argument),
                firstResult!!.error,
            )
            assertEquals(
                ResourceCache.SuccessfulResponse(downloadValueResult.value, argument),
                secondResult!!.value,
            )
        }

    /**
     * This should be fixed so that instead of awaiting requests being canceled,
     * they attempt to get the resource themselves if the ongoing request is canceled.
     */
    @Test
    fun `awaiting duplicate async request gets canceled if original is canceled`() =
        coroutineRule.runBlockingTest {
            val argument = "url"
            coEvery { RoadShieldDownloader.download(argument) } coAnswers downloadValueAsyncAnswer

            var firstResult: CacheResult? = null
            var secondResult: CacheResult? = null
            pauseDispatcher {
                val original = launch {
                    firstResult = cache.getOrRequest(argument)
                }
                // let first run by not finish yet
                advanceTimeBy(RequestDelay - 100)
                launch {
                    secondResult = cache.getOrRequest(argument)
                }
                // cancel the first one
                original.cancel()
            }

            coVerify(exactly = 1) {
                RoadShieldDownloader.download(argument)
            }

            // all request get canceled
            assertEquals(
                ResourceCache.RequestError(ResourceCache.CANCELED_MESSAGE, null),
                firstResult!!.error,
            )
            assertEquals(
                ResourceCache.RequestError(ResourceCache.CANCELED_MESSAGE, null),
                secondResult!!.error,
            )
        }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldDownloader)
    }
}
