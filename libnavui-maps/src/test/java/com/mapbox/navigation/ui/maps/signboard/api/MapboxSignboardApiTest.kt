package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.api.signboard.SignboardReadyCallback
import com.mapbox.navigation.ui.base.model.signboard.SignboardState
import com.mapbox.navigation.ui.maps.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.signboard.SignboardResult
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

    private val callback: SignboardReadyCallback = mockk(relaxed = true)
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
        val messageSlot = slot<SignboardState.Signboard.Empty>()

        signboardApi.generateSignboard(bannerInstructions, callback)

        verify(exactly = 1) { callback.onUnavailable(capture(messageSlot)) }
    }

    @Test
    fun `process result incorrect result for action`() {
        val mockResult = SignboardResult.SignboardRequest(mockk())
        val mockAction = SignboardAction.CheckSignboardAvailability(bannerInstructions)
        every { SignboardProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<SignboardState.Signboard.Error>()
        val expectedState = SignboardState.Signboard.Error(
            "Inappropriate result $mockResult emitted for $mockAction processed."
        )

        signboardApi.generateSignboard(bannerInstructions, callback)

        verify(exactly = 1) { callback.onError(capture(messageSlot)) }
        assertEquals(expectedState.exception, messageSlot.captured.exception)
    }

    @Test
    fun `process state signboard available signboard empty`() {
        val mockUrl = "https//abc.mapbox.com"
        val mockUrlWithAccessToken = "https//abc.mapbox.com?access_token=pk.1234"
        val httpResponseCallbackSlot = slot<HttpResponseCallback>()
        val mockRequest = mockk<HttpRequest>()
        val mockError = "No data available"
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
        val mockResponseData = mockk<Expected<HttpResponseData?, HttpRequestError?>>()
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
        val messageSlot = slot<SignboardState.Signboard.Empty>()

        signboardApi.generateSignboard(bannerInstructions, callback)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { callback.onUnavailable(capture(messageSlot)) }
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
        val mockResponseData = mockk<Expected<HttpResponseData?, HttpRequestError?>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.Signboard.Failure> {
            every { error } returns mockError
        }
        val expectedState = mockk<SignboardState.Signboard.Error> {
            every { exception } returns mockError
        }
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<SignboardState.Signboard.Error>()

        signboardApi.generateSignboard(bannerInstructions, callback)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { callback.onError(capture(messageSlot)) }
        assertEquals(expectedState.exception, messageSlot.captured.exception)
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
        val mockResponseData = mockk<Expected<HttpResponseData?, HttpRequestError?>>()
        val mockResponse = mockk<HttpResponse> {
            every { result } returns mockResponseData
            every { request } returns mockRequest
        }
        val mockResult = mockk<SignboardResult.Signboard.Success> {
            every { data } returns mockData
        }
        val expectedState = mockk<SignboardState.Signboard.Available> {
            every { bytes } returns mockData
        }
        every {
            SignboardProcessor.process(
                SignboardAction.ProcessSignboardResponse(mockResponseData)
            )
        } returns mockResult
        val messageSlot = slot<SignboardState.Signboard.Available>()

        signboardApi.generateSignboard(bannerInstructions, callback)
        httpResponseCallbackSlot.captured.run(mockResponse)

        verify(exactly = 1) { callback.onAvailable(capture(messageSlot)) }
        assertEquals(expectedState.bytes, messageSlot.captured.bytes)
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
        val mockFailure = SignboardState.Signboard.Error("Canceled")
        val messageSlot = slot<SignboardState.Signboard.Error>()
        val latch = CountDownLatch(1)
        every { callback.onError(capture(messageSlot)) } answers { latch.countDown() }

        signboardApi.generateSignboard(bannerInstructions, callback)
        signboardApi.cancelAll()
        latch.await(300, TimeUnit.MILLISECONDS)

        verify(exactly = 1) { callback.onError(any()) }
        assertEquals(mockFailure.exception, messageSlot.captured.exception)
        mockWebServer.shutdown()
    }
}
