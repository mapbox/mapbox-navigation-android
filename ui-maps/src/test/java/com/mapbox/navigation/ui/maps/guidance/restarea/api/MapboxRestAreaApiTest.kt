package com.mapbox.navigation.ui.maps.guidance.restarea.api

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxServices
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollectionType
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.toDataRef
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.base.util.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.base.util.resource.ResourceLoader
import com.mapbox.navigation.ui.maps.guidance.restarea.RestAreaAction
import com.mapbox.navigation.ui.maps.guidance.restarea.RestAreaProcessor
import com.mapbox.navigation.ui.maps.guidance.restarea.RestAreaResult
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapError
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapValue
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxRestAreaApiTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val consumer:
        MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>> =
            mockk(relaxed = true)
    private val bannerInstructions: BannerInstructions = mockk()
    private val restStop1: RestStop = mockk {
        every { id } returns "A1B2C3"
        every { restStopType } returns RestStopType.SERVICE_AREA
        every { name } returns "Service Area"
        every { guideMapUri } returns "https//abc.mapbox.com"
    }
    private val tollBooth: TollCollection = mockk {
        every { id } returns "A2B3C4"
        every { tollCollectionType } returns TollCollectionType.TOLL_BOOTH
        every { name } returns "Toll Booth"
    }
    private val routeProgress: RouteProgress = mockk()

    private lateinit var sapaApi: MapboxRestAreaApi
    private lateinit var mockResourceLoader: ResourceLoader

    @Before
    fun setUp() {
        mockkObject(RestAreaProcessor)
        mockkObject(ResourceLoaderFactory)
        mockkStatic(MapboxOptionsUtil::class)
        every { MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS) } returns "pk.1234"

        mockResourceLoader = mockk(relaxed = true)
        every { ResourceLoaderFactory.getInstance() } returns mockResourceLoader

        sapaApi = MapboxRestAreaApi()
    }

    @After
    fun tearDown() {
        unmockkObject(RestAreaProcessor)
        unmockkObject(ResourceLoaderFactory)
        unmockkStatic(MapboxOptionsUtil::class)
    }

    @Test
    fun `process banner instruction guide map unavailable`() {
        every {
            RestAreaProcessor.process(
                RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions),
            )
        } returns RestAreaResult.RestAreaMapUnavailable
        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No service/parking area guide map available.",
            messageSlot.captured.error!!.message,
        )
    }

    @Test
    fun `process rest stop guide map unavailable`() {
        every {
            RestAreaProcessor.process(
                RestAreaAction.CheckUpcomingRestStop(routeProgress),
            )
        } returns RestAreaResult.RestAreaMapUnavailable
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = tollBooth,
                distanceToStart = null,
                distanceInfo = null,
            ),
        )
        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No service/parking area guide map available.",
            messageSlot.captured.error!!.message,
        )
    }

    @Test
    fun `process rest stop guide map unavailable when distanceToStart is null`() {
        every {
            RestAreaProcessor.process(
                RestAreaAction.CheckUpcomingRestStop(routeProgress),
            )
        } returns RestAreaResult.RestAreaMapUnavailable
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = restStop1,
                distanceToStart = null,
                distanceInfo = null,
            ),
        )
        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No service/parking area guide map available.",
            messageSlot.captured.error!!.message,
        )
    }

    @Test
    fun `process rest stop guide map unavailable when distanceToStart is less than zero`() {
        every {
            RestAreaProcessor.process(
                RestAreaAction.CheckUpcomingRestStop(routeProgress),
            )
        } returns RestAreaResult.RestAreaMapUnavailable
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = restStop1,
                distanceToStart = -1.0,
                distanceInfo = null,
            ),
        )
        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "No service/parking area guide map available.",
            messageSlot.captured.error!!.message,
        )
    }

    @Test
    fun `process banner instruction result incorrect result for action`() {
        val mockResult = RestAreaResult.RestAreaMapRequest(mockk())
        val mockAction = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)
        every { RestAreaProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $mockResult emitted for $mockAction.",
            messageSlot.captured.error!!.message,
        )
    }

    @Test
    fun `process rest stop result incorrect result for action`() {
        val mockResult = RestAreaResult.RestAreaMapRequest(mockk())
        val mockAction = RestAreaAction.CheckUpcomingRestStop(routeProgress)
        every { RestAreaProcessor.process(mockAction) } returns mockResult
        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(
            "Inappropriate $mockResult emitted for $mockAction.",
            messageSlot.captured.error!!.message,
        )
    }

    @Test
    fun `process banner instruction guide map available guide map empty`() {
        val expectedError = "No service/parking area guide map available."
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Empty,
        )

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.message)
    }

    @Test
    fun `process rest stop guide map available guide map empty`() {
        every { restStop1.objectType } returns RestStopType.SERVICE_AREA
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = restStop1,
                distanceToStart = 1.0,
                distanceInfo = null,
            ),
        )
        val expectedError = "No service/parking area guide map available."
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Empty,
        )

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.message)
    }

    @Test
    fun `process banner instruction guide map available guide map failure`() {
        val expectedError = "Resource is missing"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgResult = mockk<RestAreaResult.RestAreaMapSvg.Failure> {
            every { error } returns expectedError
        }

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = svgResult,
        )

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.message)
    }

    @Test
    fun `process rest stop guide map available guide map failure`() {
        every { restStop1.objectType } returns RestStopType.SERVICE_AREA
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = restStop1,
                distanceToStart = 1.0,
                distanceInfo = null,
            ),
        )
        val expectedError = "Resource is missing"
        val url = "https//abc.mapbox.com"
        val loadRequest = mockk<ResourceLoadRequest>()
        val loadResponse = mockk<Expected<ResourceLoadError, ResourceLoadResult>>()
        val svgResult = mockk<RestAreaResult.RestAreaMapSvg.Failure> {
            every { error } returns expectedError
        }

        givenResourceLoaderResponse(
            request = loadRequest,
            response = loadResponse,
        )
        givenProcessorResults(
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = svgResult,
        )

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.message)
    }

    @Test
    fun `process banner instruction guide map available guide map svg success parse fail`() {
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
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Success(svgData),
            parseSvgToBitmap = RestAreaResult.RestAreaBitmap.Failure(parserFailure.error!!, null),
        )

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.message)
        clearAllMocks(answers = false)

        // use new token
        val newToken = "new.token"
        givenProcessorResults(
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Success(svgData),
            parseSvgToBitmap = RestAreaResult.RestAreaBitmap.Failure(parserFailure.error!!, null),
            token = newToken,
        )

        every { MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS) } returns newToken
        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        val messageSlot2 = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot2)) }
        assertEquals(expectedError, messageSlot2.captured.error!!.message)
    }

    @Test
    fun `process rest stop guide map available guide map svg success parse fail`() {
        every { restStop1.objectType } returns RestStopType.SERVICE_AREA
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = restStop1,
                distanceToStart = 1.0,
                distanceInfo = null,
            ),
        )
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
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Success(svgData),
            parseSvgToBitmap = RestAreaResult.RestAreaBitmap.Failure(parserFailure.error!!, null),
        )

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedError, messageSlot.captured.error!!.message)
    }

    @Test
    fun `process banner instruction guide map available guide map svg success parse success`() {
        mockkObject(SvgUtil)
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
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Success(svgData),
            parseSvgToBitmap = RestAreaResult.RestAreaBitmap.Success(parserSuccess.value!!),
        )
        every { SvgUtil.renderAsBitmapWithWidth(any(), any(), any()) } returns parserSuccess.value!!

        sapaApi.generateRestAreaGuideMap(bannerInstructions, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedBitmap, messageSlot.captured.value!!.bitmap)
        unmockkObject(SvgUtil)
    }

    @Test
    fun `process rest stop guide map available guide map svg success parse success`() {
        every { restStop1.objectType } returns RestStopType.SERVICE_AREA
        every { routeProgress.upcomingRoadObjects } returns listOf(
            RoadObjectFactory.buildUpcomingRoadObject(
                roadObject = restStop1,
                distanceToStart = 1.0,
                distanceInfo = null,
            ),
        )
        mockkObject(SvgUtil)
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
            checkSapaAvailability = RestAreaResult.RestAreaMapAvailable(url),
            prepareSapaMapRequest = RestAreaResult.RestAreaMapRequest(loadRequest),
            processSapaMapResponse = RestAreaResult.RestAreaMapSvg.Success(svgData),
            parseSvgToBitmap = RestAreaResult.RestAreaBitmap.Success(parserSuccess.value!!),
        )
        every { SvgUtil.renderAsBitmapWithWidth(any(), any(), any()) } returns parserSuccess.value!!

        sapaApi.generateUpcomingRestAreaGuideMap(routeProgress, consumer)

        val messageSlot = slot<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>()
        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(expectedBitmap, messageSlot.captured.value!!.bitmap)
        unmockkObject(SvgUtil)
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
        checkSapaAvailability: RestAreaResult,
        prepareSapaMapRequest: RestAreaResult? = null,
        processSapaMapResponse: RestAreaResult? = null,
        parseSvgToBitmap: RestAreaResult? = null,
        token: String = "pk.1234",
    ) {
        every {
            RestAreaProcessor.process(
                RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions),
            )
        } returns checkSapaAvailability

        every {
            RestAreaProcessor.process(
                RestAreaAction.CheckUpcomingRestStop(routeProgress),
            )
        } returns checkSapaAvailability

        if (prepareSapaMapRequest != null) {
            every {
                RestAreaProcessor.process(
                    match {
                        it is RestAreaAction.PrepareRestAreaMapRequest &&
                            it.sapaMapUrl.contains("access_token=$token")
                    },
                )
            } returns prepareSapaMapRequest
        }

        if (processSapaMapResponse != null) {
            every {
                RestAreaProcessor.process(ofType(RestAreaAction.ProcessRestAreaMapResponse::class))
            } returns processSapaMapResponse
        }

        if (parseSvgToBitmap != null) {
            every {
                RestAreaProcessor.process(ofType(RestAreaAction.ParseSvgToBitmap::class))
            } returns parseSvgToBitmap
        }
    }
}
