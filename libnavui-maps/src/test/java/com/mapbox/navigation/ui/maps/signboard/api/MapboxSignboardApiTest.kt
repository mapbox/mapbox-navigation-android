package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.signboard.model.SignboardValue
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class MapboxSignboardApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val consumer:
        MapboxNavigationConsumer<Expected<SignboardValue, SignboardError>> = mockk(relaxed = true)
    private val bannerInstructions: BannerInstructions = mockk()
    private val signboardApi = MapboxSignboardApi("pk.1234")

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        mockkObject(SignboardProcessor)
        mockkObject(CommonSingletonModuleProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(SignboardProcessor)
        unmockkObject(CommonSingletonModuleProvider)
        unmockkObject(ThreadController)
    }

    @Test
    fun `process state signboard unavailable`() {
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions)
            )
        } returns SignboardResult.SignboardUnavailable
        val messageSlot = slot<Expected.Failure<SignboardError>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No signboard available for current maneuver.",
            messageSlot.captured.error.errorMessage
        )
    }

    @Test
    fun `process result incorrect result for action`() {
        val mockResult = SignboardResult.SignboardRequest(mockk())
        val mockAction = SignboardAction.CheckSignboardAvailability(bannerInstructions)
        every { SignboardProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<Expected.Failure<SignboardError>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $mockResult emitted for $mockAction.",
            messageSlot.captured.error.errorMessage
        )
    }

    @Test
    fun `process state signboard available signboard empty`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com?access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val expectedError = "No signboard available for current maneuver."
        val mockHttpService = mockk<HttpServiceInterface>() {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions)
            )
        } returns SignboardResult.SignboardAvailable(mockUrl)
        every {
            SignboardProcessor.process(
                SignboardAction.PrepareSignboardRequest(mockUrlWithAccessToken)
            )
        } returns SignboardResult.SignboardRequest(mockRequest)
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpResponseData?, HttpRequestError?>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.Signboard.Empty>()
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<Expected.Failure<SignboardError>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error.errorMessage)
    }

    @Test
    fun `process state signboard available signboard failure`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com?access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val mockError = "Resource is missing"
        val mockHttpService = mockk<HttpServiceInterface>() {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions)
            )
        } returns SignboardResult.SignboardAvailable(mockUrl)
        every {
            SignboardProcessor.process(
                SignboardAction.PrepareSignboardRequest(mockUrlWithAccessToken)
            )
        } returns SignboardResult.SignboardRequest(mockRequest)
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpResponseData?, HttpRequestError?>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.Signboard.Failure> {
            every { error } returns mockError
        }
        val expected = mockk<Expected.Failure<SignboardError>> {
            every { error.errorMessage } returns mockError
        }
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<Expected.Failure<SignboardError>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.error.errorMessage, messageSlot.captured.error.errorMessage)
    }

    @Test
    fun `process state signboard available signboard success`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com?access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val mockHttpService = mockk<HttpServiceInterface>() {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions)
            )
        } returns SignboardResult.SignboardAvailable(mockUrl)
        every {
            SignboardProcessor.process(
                SignboardAction.PrepareSignboardRequest(mockUrlWithAccessToken)
            )
        } returns SignboardResult.SignboardRequest(mockRequest)
        val mockData = byteArrayOf(-12, 12, 34, 55, -45)
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpResponseData?, HttpRequestError?>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.Signboard.Success> {
            every { data } returns mockData
        }
        val expected = mockk<Expected.Success<SignboardValue>> {
            every { value.bytes } returns mockData
        }
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<Expected.Success<SignboardValue>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.value.bytes, messageSlot.captured.value.bytes)
    }

    @Test
    fun `process request signboard request cancel`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()
        val mockUrl = "https://api-guidance-views-staging.tilestream.net/guidance-views/" +
            "v1/1603670400/signboard/SI_1241914001A2"
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions)
            )
        } returns SignboardResult.SignboardAvailable(mockWebServer.url(mockUrl).toString())
        val mockFailure = Expected.Failure(SignboardError("Canceled", null))
        val messageSlot = slot<Expected.Failure<SignboardError>>()
        val latch = CountDownLatch(1)
        every { consumer.accept(capture(messageSlot)) } answers { latch.countDown() }

        signboardApi.generateSignboard(bannerInstructions, consumer)
        signboardApi.cancelAll()
        latch.await(300, TimeUnit.MILLISECONDS)

        verify(exactly = 1) { consumer.accept(any()) }
        assertEquals(mockFailure.error.errorMessage, messageSlot.captured.error.errorMessage)
        mockWebServer.shutdown()
    }
}
