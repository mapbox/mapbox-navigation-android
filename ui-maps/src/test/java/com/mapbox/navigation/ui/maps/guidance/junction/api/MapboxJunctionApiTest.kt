@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxServices
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.toDataRef
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionAction
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionProcessor
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionResult
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionViewData
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionViewFormat
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
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

private typealias JunctionValueConsumer =
    MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>

private typealias JunctionViewDataConsumer =
    MapboxNavigationConsumer<Expected<JunctionError, JunctionViewData>>

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxJunctionApiTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val bannerInstructions: BannerInstructions = mockk()

    private lateinit var junctionApi: MapboxJunctionApi
    private lateinit var mockResourceLoader: ResourceLoader

    @Before
    fun setUp() {
        mockkObject(JunctionProcessor)
        mockkObject(ResourceLoaderFactory)
        mockkStatic(BitmapFactory::class)
        mockkStatic(MapboxRasterToBitmapParser::class)
        mockkStatic(MapboxOptionsUtil::class)
        every {
            MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        } returns DIRECTIONS_TOKEN

        mockResourceLoader = mockk(relaxed = true)
        every { ResourceLoaderFactory.getInstance() } returns mockResourceLoader

        junctionApi = MapboxJunctionApi()
    }

    @After
    fun tearDown() {
        unmockkObject(JunctionProcessor)
        unmockkObject(ResourceLoaderFactory)
        unmockkStatic(BitmapFactory::class)
        unmockkStatic(MapboxRasterToBitmapParser::class)
        unmockkStatic(MapboxOptionsUtil::class)
    }

    @Test
    fun `process state junction unavailable`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val expectedError = "No junction available for current maneuver."
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionUnavailable,
        )

        junctionApi.generateJunction(bannerInstructions, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process result incorrect result for action`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val mockResult = JunctionResult.JunctionRequest(mockk())
        val mockAction = JunctionAction.CheckJunctionAvailability(bannerInstructions)
        every { JunctionProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()

        junctionApi.generateJunction(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $mockResult emitted for $mockAction.",
            messageSlot.captured.error!!.errorMessage,
        )
    }

    @Test
    fun `process state junction available junction empty`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val expectedError = "No junction available for current maneuver."
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val rasterResult = mockk<JunctionResult.JunctionRaster.Empty>()

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = rasterResult,
        )

        junctionApi.generateJunction(bannerInstructions, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state junction available junction failure`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val expectedError = "Resource is missing"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val rasterResult = mockk<JunctionResult.JunctionRaster.Failure> {
            every { error } returns expectedError
        }

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = rasterResult,
        )

        junctionApi.generateJunction(bannerInstructions, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state junction available junction raster success parse fail`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val expectedError = "Error parsing raster to bitmap as raster is empty"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val rasterData = byteArrayOf().toDataRef()
        val contentType = "image/png"
        val parserFailure = ExpectedFactory.createError<String, Bitmap>(expectedError)

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = JunctionResult.JunctionRaster.Success(
                rasterData,
                contentType,
            ),
            parseRasterToBitmap = JunctionResult.JunctionBitmap.Failure(parserFailure.error!!),
        )

        junctionApi.generateJunction(bannerInstructions, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state junction available junction raster success parse success`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val expectedBitmap = mockk<Bitmap>()
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val rasterData = byteArrayOf(12, -12, 23, 45, 67, 65, 44, 45, 12, 34, 45, 56, 76)
            .toDataRef()
        val contentType = "image/png"
        val parseSuccess: Expected<String, Bitmap> = ExpectedFactory.createValue(expectedBitmap)

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = JunctionResult.JunctionRaster.Success(
                rasterData,
                contentType,
            ),
            parseRasterToBitmap = JunctionResult.JunctionBitmap.Success(parseSuccess.value!!),
        )

        every { BitmapFactory.decodeStream(any()) } returns expectedBitmap
        every { MapboxRasterToBitmapParser.parse(rasterData) } returns parseSuccess

        junctionApi.generateJunction(bannerInstructions, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedBitmap, messageSlot.captured.value!!.bitmap)
        verify(exactly = 1) {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(buildExpectedJunctionUrl(url, null)),
            )
        }

        clearAllMocks(answers = false)

        // use new token
        val newToken = "new.token"
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = JunctionResult.JunctionRaster.Success(
                rasterData,
                contentType,
            ),
            parseRasterToBitmap = JunctionResult.JunctionBitmap.Success(parseSuccess.value!!),
            token = newToken,
        )

        every { MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS) } returns newToken
        junctionApi.generateJunction(bannerInstructions, consumer)

        val messageSlot2 = slot<Expected<JunctionError, JunctionValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot2)) }
        assertEquals(expectedBitmap, messageSlot2.captured.value!!.bitmap)
        verify(exactly = 1) {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(
                    buildExpectedJunctionUrl(url, null, newToken),
                ),
            )
        }
    }

    @Ignore("Make this test an instrumentation test to avoid UnsatisfiedLinkError from Common 11+")
    @Test
    fun `process request junction request cancel`() {
        val consumer: JunctionValueConsumer = mockk(relaxed = true)

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()
        val mockUrl =
            "https://api.mapbox.com/guidance-views/v1/1596240000/jct/CB273101?arrow_ids=CB27310A"
        every {
            JunctionProcessor.process(
                JunctionAction.CheckJunctionAvailability(bannerInstructions),
            )
        } returns JunctionResult.JunctionAvailable(mockWebServer.url(mockUrl).toString())
        val mockFailure: Expected<JunctionError, JunctionValue> =
            ExpectedFactory.createError(JunctionError("Canceled", null))
        val messageSlot = slot<Expected<JunctionError, JunctionValue>>()
        val latch = CountDownLatch(1)
        every { consumer.accept(capture(messageSlot)) } answers { latch.countDown() }

        junctionApi.generateJunction(bannerInstructions, consumer)
        junctionApi.cancelAll()
        latch.await(300, TimeUnit.MILLISECONDS)

        verify(exactly = 1) { consumer.accept(any()) }
        assertEquals(mockFailure.error!!.errorMessage, messageSlot.captured.error!!.errorMessage)
        mockWebServer.shutdown()
    }

    @Test
    fun `process state junction unavailable for svg request`() {
        val consumer: JunctionViewDataConsumer = mockk(relaxed = true)

        val expectedError = "No junction available for current maneuver."
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionUnavailable,
        )

        junctionApi.generateJunction(bannerInstructions, JunctionViewFormat.Companion.SVG, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionViewData>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state junction available junction failure for svg`() {
        val consumer: JunctionViewDataConsumer = mockk(relaxed = true)

        val expectedError = "Resource is missing"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val rasterResult = mockk<JunctionResult.JunctionRaster.Failure> {
            every { error } returns expectedError
        }

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = rasterResult,
        )

        junctionApi.generateJunction(bannerInstructions, JunctionViewFormat.SVG, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionViewData>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.errorMessage)
    }

    @Test
    fun `process state junction available for svg`() {
        val consumer: JunctionViewDataConsumer = mockk(relaxed = true)

        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgData = byteArrayOf(12, -12, 23, 45, 67, 65, 44, 45, 12, 34, 45, 56, 76)
        val contentType = "image/svg+xml"

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = JunctionResult.JunctionRaster.Success(
                svgData.toDataRef(),
                contentType,
            ),
        )

        junctionApi.generateJunction(bannerInstructions, JunctionViewFormat.SVG, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionViewData>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }

        val junctionViewData = JunctionViewData(svgData, JunctionViewData.ResponseFormat.SVG)
        assertEquals(junctionViewData, messageSlot.captured.value)

        verify(exactly = 1) {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(
                    buildExpectedJunctionUrl(url, JunctionViewFormat.SVG),
                ),
            )
        }
    }

    @Test
    fun `process state junction available for explicit png`() {
        val consumer: JunctionViewDataConsumer = mockk(relaxed = true)

        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgData = byteArrayOf(12, -12, 23, 45, 67, 65, 44, 45, 12, 34, 45, 56, 76)
        val contentType = "image/png"

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkJunctionAvailability = JunctionResult.JunctionAvailable(url),
            prepareJunctionRequest = JunctionResult.JunctionRequest(loadRequest),
            processJunctionResponse = JunctionResult.JunctionRaster.Success(
                svgData.toDataRef(),
                contentType,
            ),
        )

        junctionApi.generateJunction(bannerInstructions, JunctionViewFormat.PNG, consumer)

        val messageSlot = slot<Expected<JunctionError, JunctionViewData>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }

        val junctionViewData = JunctionViewData(svgData, JunctionViewData.ResponseFormat.PNG)
        assertEquals(junctionViewData, messageSlot.captured.value)

        verify(exactly = 1) {
            JunctionProcessor.process(
                JunctionAction.PrepareJunctionRequest(
                    buildExpectedJunctionUrl(url, JunctionViewFormat.PNG),
                ),
            )
        }
    }

    private fun buildExpectedJunctionUrl(
        baseUrl: String,
        @JunctionViewFormat format: String?,
        token: String? = DIRECTIONS_TOKEN,
    ): String {
        return Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("access_token", token)
            .apply {
                if (format != null) {
                    appendQueryParameter("image_format", format)
                }
            }
            .build()
            .toString()
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
        checkJunctionAvailability: JunctionResult,
        prepareJunctionRequest: JunctionResult? = null,
        processJunctionResponse: JunctionResult? = null,
        parseRasterToBitmap: JunctionResult? = null,
        token: String = DIRECTIONS_TOKEN,
    ) {
        every {
            JunctionProcessor.process(JunctionAction.CheckJunctionAvailability(bannerInstructions))
        } returns checkJunctionAvailability

        if (prepareJunctionRequest != null) {
            every {
                JunctionProcessor.process(
                    match {
                        it is JunctionAction.PrepareJunctionRequest &&
                            it.junctionUrl.contains("access_token=$token")
                    },
                )
            } returns prepareJunctionRequest
        }

        if (processJunctionResponse != null) {
            every {
                JunctionProcessor.process(ofType(JunctionAction.ProcessJunctionResponse::class))
            } returns processJunctionResponse
        }

        if (parseRasterToBitmap != null) {
            every {
                JunctionProcessor.process(ofType(JunctionAction.ParseRasterToBitmap::class))
            } returns parseRasterToBitmap
        }
    }

    private companion object {
        const val DIRECTIONS_TOKEN = "pk.1234"
    }
}
