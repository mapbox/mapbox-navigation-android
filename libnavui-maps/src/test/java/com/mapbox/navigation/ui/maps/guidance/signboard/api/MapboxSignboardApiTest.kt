package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.graphics.Bitmap
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
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class MapboxSignboardApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    val mockParser: SvgToBitmapParser = mockk(relaxed = true)
    private val consumer:
        MapboxNavigationConsumer<Expected<SignboardValue, SignboardError>> = mockk(relaxed = true)
    private val bannerInstructions: BannerInstructions = mockk()
    private val signboardOptions = mockk<MapboxSignboardOptions>()
    private val signboardApi = MapboxSignboardApi("pk.1234", mockParser, signboardOptions)

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
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.SignboardSvg.Empty>()
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
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.SignboardSvg.Failure> {
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
    fun `process state signboard available signboard svg success parse fail`() {
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
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockSvgResult = mockk<SignboardResult.SignboardSvg.Success> {
            every { data } returns mockData
        }
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockSvgResult
        val mockParseFailure = Expected.Failure("This is an error")
        every { mockParser.parse(mockData, signboardOptions) } returns mockParseFailure
        every {
            SignboardProcessor.process(
                SignboardAction.ParseSvgToBitmap(mockData, mockParser, signboardOptions)
            )
        } returns SignboardResult.SignboardBitmap.Failure("This is an error")
        val expected = mockk<Expected.Failure<SignboardError>> {
            every { error.errorMessage } returns "This is an error"
        }
        val messageSlot = slot<Expected.Failure<SignboardError>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.error.errorMessage, messageSlot.captured.error.errorMessage)
    }

    @Test
    fun `process state signboard available signboard svg success parse success`() {
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
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockSvgResult = mockk<SignboardResult.SignboardSvg.Success> {
            every { data } returns mockData
        }
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockSvgResult
        val mockBitmap = mockk<Bitmap>()
        val mockParseSuccess = Expected.Success(mockBitmap)
        every { mockParser.parse(mockData, signboardOptions) } returns mockParseSuccess
        every {
            SignboardProcessor.process(
                SignboardAction.ParseSvgToBitmap(mockData, mockParser, signboardOptions)
            )
        } returns SignboardResult.SignboardBitmap.Success(mockBitmap)
        val expected = mockk<Expected.Success<SignboardValue>> {
            every { value.bitmap } returns mockBitmap
        }
        val messageSlot = slot<Expected.Success<SignboardValue>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.value.bitmap, messageSlot.captured.value.bitmap)
    }

    @Ignore("Make this test an instrumentation test to avoid UnsatisfiedLinkError from Common 11+")
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
