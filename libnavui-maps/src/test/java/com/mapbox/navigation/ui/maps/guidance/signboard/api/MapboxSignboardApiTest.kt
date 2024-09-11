package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxServices
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.toDataRef
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoaderFactory
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxSignboardApiTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockParser: SvgToBitmapParser = mockk(relaxed = true)
    private val consumer:
        MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>> = mockk(relaxed = true)
    private val bannerInstructions: BannerInstructions = mockk()
    private val signboardOptions = mockk<MapboxSignboardOptions>()

    private lateinit var signboardApi: MapboxSignboardApi
    private lateinit var mockResourceLoader: ResourceLoader

    @Before
    fun setUp() {
        mockkObject(SignboardProcessor)
        mockkObject(ResourceLoaderFactory)
        mockkStatic(MapboxOptionsUtil::class)
        every { MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS) } returns "pk.1234"

        mockResourceLoader = mockk(relaxed = true)
        every { ResourceLoaderFactory.getInstance() } returns mockResourceLoader

        signboardApi = MapboxSignboardApi(mockParser, signboardOptions)
    }

    @After
    fun tearDown() {
        unmockkObject(SignboardProcessor)
        unmockkObject(ResourceLoaderFactory)
        unmockkStatic(MapboxOptionsUtil::class)
    }

    @Test
    fun `process state signboard unavailable`() {
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions),
            )
        } returns SignboardResult.SignboardUnavailable
        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No signboard available for current maneuver.",
            messageSlot.captured.error!!.errorMessage,
        )
    }

    @Test
    fun `process result incorrect result for action`() {
        val mockResult = SignboardResult.SignboardRequest(mockk())
        val mockAction = SignboardAction.CheckSignboardAvailability(bannerInstructions)
        every { SignboardProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()

        signboardApi.generateSignboard(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $mockResult emitted for $mockAction.",
            messageSlot.captured.error!!.errorMessage,
        )
    }

    @Test
    fun `process state signboard available signboard empty`() {
        val expectedError = "No signboard available for current maneuver."
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgResult = mockk<SignboardResult.SignboardSvg.Empty>()

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSignboardAvailability = SignboardResult.SignboardAvailable(url),
            prepareSignboardRequest = SignboardResult.SignboardRequest(loadRequest),
            processSignboardResponse = svgResult,
        )

        signboardApi.generateSignboard(bannerInstructions, consumer)

        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state signboard available signboard failure`() {
        val expectedError = "Resource is missing"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgResult = mockk<SignboardResult.SignboardSvg.Failure> {
            every { error } returns expectedError
        }

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSignboardAvailability = SignboardResult.SignboardAvailable(url),
            prepareSignboardRequest = SignboardResult.SignboardRequest(loadRequest),
            processSignboardResponse = svgResult,
        )

        signboardApi.generateSignboard(bannerInstructions, consumer)

        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state signboard available signboard svg success parse fail`() {
        val expectedError = "This is an error"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgData = byteArrayOf().toDataRef()
        val parserFailure = ExpectedFactory.createError<String, Bitmap>(expectedError)

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSignboardAvailability = SignboardResult.SignboardAvailable(url),
            prepareSignboardRequest = SignboardResult.SignboardRequest(loadRequest),
            processSignboardResponse = SignboardResult.SignboardSvg.Success(svgData),
            parseSvgToBitmap = SignboardResult.SignboardBitmap.Failure(parserFailure.error!!),
        )

        signboardApi.generateSignboard(bannerInstructions, consumer)

        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state signboard available signboard svg success parse success`() {
        val expectedBitmap = mockk<Bitmap>()
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgData = byteArrayOf(-12, 12, 34, 55, -45).toDataRef()
        val parserSuccess = ExpectedFactory.createValue<String, Bitmap>(expectedBitmap)

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSignboardAvailability = SignboardResult.SignboardAvailable(url),
            prepareSignboardRequest = SignboardResult.SignboardRequest(loadRequest),
            processSignboardResponse = SignboardResult.SignboardSvg.Success(svgData),
            parseSvgToBitmap = SignboardResult.SignboardBitmap.Success(parserSuccess.value!!),
        )
        every { mockParser.parse(svgData.buffer, signboardOptions) } returns parserSuccess

        signboardApi.generateSignboard(bannerInstructions, consumer)

        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedBitmap, messageSlot.captured.value!!.bitmap)
        clearAllMocks(answers = false)

        // use new token
        val newToken = "new.token"
        givenProcessorResults(
            checkSignboardAvailability = SignboardResult.SignboardAvailable(url),
            prepareSignboardRequest = SignboardResult.SignboardRequest(loadRequest),
            processSignboardResponse = SignboardResult.SignboardSvg.Success(svgData),
            parseSvgToBitmap = SignboardResult.SignboardBitmap.Success(parserSuccess.value!!),
            token = newToken,
        )
        every { MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS) } returns newToken

        signboardApi.generateSignboard(bannerInstructions, consumer)
        val messageSlot2 = slot<Expected<SignboardError, SignboardValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot2)) }
        assertEquals(expectedBitmap, messageSlot2.captured.value!!.bitmap)
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
                SignboardAction.CheckSignboardAvailability(bannerInstructions),
            )
        } returns SignboardResult.SignboardAvailable(mockWebServer.url(mockUrl).toString())
        val mockFailure: Expected<SignboardError, SignboardValue> =
            ExpectedFactory.createError(SignboardError("Canceled", null))
        val messageSlot = slot<Expected<SignboardError, SignboardValue>>()
        val latch = CountDownLatch(1)
        every { consumer.accept(capture(messageSlot)) } answers { latch.countDown() }

        signboardApi.generateSignboard(bannerInstructions, consumer)
        signboardApi.cancelAll()
        latch.await(300, TimeUnit.MILLISECONDS)

        verify(exactly = 1) { consumer.accept(any()) }
        assertEquals(mockFailure.error!!.errorMessage, messageSlot.captured.error!!.errorMessage)
        mockWebServer.shutdown()
    }

    private fun givenResourceLoaderResponse(
        request: ResourceLoadRequest,
        response: Expected<ResourceLoadError, ResourceLoadResult>,
    ) {
        val loadCallbackSlot = slot<ResourceLoadCallback>()
        every { mockResourceLoader.load(request, capture(loadCallbackSlot)) } answers {
            loadCallbackSlot.captured.onFinish(request, response)
            0L
        }
    }

    private fun givenProcessorResults(
        checkSignboardAvailability: SignboardResult,
        prepareSignboardRequest: SignboardResult? = null,
        processSignboardResponse: SignboardResult? = null,
        parseSvgToBitmap: SignboardResult? = null,
        token: String = "pk.1234",
    ) {
        every {
            SignboardProcessor.process(
                SignboardAction.CheckSignboardAvailability(bannerInstructions),
            )
        } returns checkSignboardAvailability

        if (prepareSignboardRequest != null) {
            every {
                SignboardProcessor.process(
                    match {
                        it is SignboardAction.PrepareSignboardRequest &&
                            it.signboardUrl.contains("access_token=$token")
                    },
                )
            } returns prepareSignboardRequest
        }

        if (processSignboardResponse != null) {
            every {
                SignboardProcessor.process(ofType(SignboardAction.ProcessSignboardResponse::class))
            } returns processSignboardResponse
        }

        if (parseSvgToBitmap != null) {
            every {
                SignboardProcessor.process(ofType(SignboardAction.ParseSvgToBitmap::class))
            } returns parseSvgToBitmap
        }
    }
}
