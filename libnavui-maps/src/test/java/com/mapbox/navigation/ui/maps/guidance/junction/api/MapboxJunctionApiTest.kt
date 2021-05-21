package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionAction
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionProcessor
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionResult
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
class MapboxJunctionApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val consumer:
        MapboxNavigationConsumer<Expected<JunctionValue, JunctionError>> = mockk(relaxed = true)
    private val bannerInstructions: BannerInstructions = mockk()
    private val junctionApi = MapboxJunctionApi("pk.1234")

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        mockkObject(JunctionProcessor)
        mockkObject(CommonSingletonModuleProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(JunctionProcessor)
        unmockkObject(CommonSingletonModuleProvider)
        unmockkObject(ThreadController)
    }

    @Test
    fun `process state junction unavailable`() {
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions)
            )
        } returns JunctionResult.JunctionUnavailable
        val messageSlot = slot<Expected.Failure<JunctionError>>()

        junctionApi.generateJunction(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No junction available for current maneuver.",
            messageSlot.captured.error.errorMessage
        )
    }

    @Test
    fun `process result incorrect result for action`() {
        val mockResult = JunctionResult.JunctionRequest(mockk())
        val mockAction = JunctionAction.CheckJunctionAvailability(bannerInstructions)
        every { JunctionProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<Expected.Failure<JunctionError>>()

        junctionApi.generateJunction(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $mockResult emitted for $mockAction.",
            messageSlot.captured.error.errorMessage
        )
    }

    @Test
    fun `process state junction available junction empty`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com&access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val expectedError = "No junction available for current maneuver."
        val mockHttpService = mockk<HttpServiceInterface> {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions)
            )
        } returns JunctionResult.JunctionAvailable(mockUrl)
        every {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(mockUrlWithAccessToken)
            )
        } returns JunctionResult.JunctionRequest(mockRequest)
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<JunctionResult.JunctionRaster.Empty>()
        every {
            JunctionProcessor.process(
                JunctionAction.ProcessJunctionResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<Expected.Failure<JunctionError>>()

        junctionApi.generateJunction(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error.errorMessage)
    }

    @Test
    fun `process state junction available junction failure`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com&access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val mockError = "Resource is missing"
        val mockHttpService = mockk<HttpServiceInterface> {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions)
            )
        } returns JunctionResult.JunctionAvailable(mockUrl)
        every {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(mockUrlWithAccessToken)
            )
        } returns JunctionResult.JunctionRequest(mockRequest)
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<JunctionResult.JunctionRaster.Failure> {
            every { error } returns mockError
        }
        val expected = mockk<Expected.Failure<JunctionError>> {
            every { error.errorMessage } returns mockError
        }
        every {
            JunctionProcessor.process(
                JunctionAction.ProcessJunctionResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<Expected.Failure<JunctionError>>()

        junctionApi.generateJunction(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.error.errorMessage, messageSlot.captured.error.errorMessage)
    }

    @Test
    fun `process state junction available junction raster success parse fail`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com&access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val mockHttpService = mockk<HttpServiceInterface> {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions)
            )
        } returns JunctionResult.JunctionAvailable(mockUrl)
        every {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(mockUrlWithAccessToken)
            )
        } returns JunctionResult.JunctionRequest(mockRequest)
        val mockData = byteArrayOf()
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockRasterResult = mockk<JunctionResult.JunctionRaster.Success> {
            every { data } returns mockData
        }
        every {
            JunctionProcessor.process(
                JunctionAction.ProcessJunctionResponse(mockResponseData)
            )
        } returns mockRasterResult
        val mockParseFailure = Expected.Failure("Error parsing raster to bitmap as raster is empty")
        mockkStatic(MapboxRasterToBitmapParser::class)
        every { MapboxRasterToBitmapParser.parse(mockData) } returns mockParseFailure
        every {
            JunctionProcessor.process(
                JunctionAction.ParseRasterToBitmap(mockData)
            )
        } returns JunctionResult.JunctionBitmap.Failure(mockParseFailure.error)
        val expected = mockk<Expected.Failure<JunctionError>> {
            every { error.errorMessage } returns mockParseFailure.error
        }
        val messageSlot = slot<Expected.Failure<JunctionError>>()

        junctionApi.generateJunction(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.error.errorMessage, messageSlot.captured.error.errorMessage)
    }

    @Test
    fun `process state junction available junction raster success parse success`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com&access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val mockHttpService = mockk<HttpServiceInterface> {
            every { request(mockRequest, capture(httpResponseCallbackSlot)) } returns 0
        }
        every { CommonSingletonModuleProvider.httpServiceInstance } returns mockHttpService
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions)
            )
        } returns JunctionResult.JunctionAvailable(mockUrl)
        every {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(mockUrlWithAccessToken)
            )
        } returns JunctionResult.JunctionRequest(mockRequest)
        val mockData = byteArrayOf(12, -12, 23, 45, 67, 65, 44, 45, 12, 34, 45, 56, 76)
        val mockResponseData =
            mockk<com.mapbox.bindgen.Expected<HttpRequestError, HttpResponseData>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockRasterResult = mockk<JunctionResult.JunctionRaster.Success> {
            every { data } returns mockData
        }
        every {
            JunctionProcessor.process(
                JunctionAction.ProcessJunctionResponse(mockResponseData)
            )
        } returns mockRasterResult
        mockkStatic(BitmapFactory::class)
        val mockBitmap = mockk<Bitmap>()
        every { BitmapFactory.decodeByteArray(mockData, 0, mockData.size) } returns mockBitmap
        val mockParseSuccess = Expected.Success(mockBitmap)
        mockkObject(MapboxRasterToBitmapParser)
        every { MapboxRasterToBitmapParser.parse(mockData) } returns mockParseSuccess
        every {
            JunctionProcessor.process(
                JunctionAction.ParseRasterToBitmap(mockData)
            )
        } returns JunctionResult.JunctionBitmap.Success(mockParseSuccess.value)
        val expected = mockk<Expected.Success<JunctionValue>> {
            every { value.bitmap } returns mockBitmap
        }
        val messageSlot = slot<Expected.Success<JunctionValue>>()

        junctionApi.generateJunction(bannerInstructions, consumer)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expected.value.bitmap, messageSlot.captured.value.bitmap)
    }

    @Ignore("Make this test an instrumentation test to avoid UnsatisfiedLinkError from Common 11+")
    @Test
    fun `process request junction request cancel`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()
        val mockUrl =
            "https://api.mapbox.com/guidance-views/v1/1596240000/jct/CB273101?arrow_ids=CB27310A"
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions)
            )
        } returns JunctionResult.JunctionAvailable(mockWebServer.url(mockUrl).toString())
        val mockFailure = Expected.Failure(JunctionError("Canceled", null))
        val messageSlot = slot<Expected.Failure<JunctionError>>()
        val latch = CountDownLatch(1)
        every { consumer.accept(capture(messageSlot)) } answers { latch.countDown() }

        junctionApi.generateJunction(bannerInstructions, consumer)
        junctionApi.cancelAll()
        latch.await(300, TimeUnit.MILLISECONDS)

        verify(exactly = 1) { consumer.accept(any()) }
        assertEquals(mockFailure.error.errorMessage, messageSlot.captured.error.errorMessage)
        mockWebServer.shutdown()
    }
}
