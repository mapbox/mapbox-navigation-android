package com.mapbox.navigation.ui.maps.guidance.restarea

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.ResourceData
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.buildRoadObject
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.buildUpcomingRoadObject
import com.mapbox.navigation.base.internal.factory.RouteProgressFactory.buildRouteProgressObject
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.ui.maps.guidance.restarea.model.MapboxRestAreaOptions
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigator.Amenity
import com.mapbox.navigator.AmenityType
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.MatchedRoadObjectLocation
import com.mapbox.navigator.Position
import com.mapbox.navigator.RoadObject
import com.mapbox.navigator.RoadObjectMetadata
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.ServiceAreaInfo
import com.mapbox.navigator.ServiceAreaType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class RestAreaProcessorTest {

    @Test
    fun `process action sapa map availability result unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable no subType component`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result unavailable no image url`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSapaSubType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = RestAreaResult.RestAreaMapUnavailable
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action sapa map availability result available`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(
            getComponentGuidanceViewTypeSapaSubTypeImageUrl("https://abc.mapbox.com&")
        )
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = RestAreaResult.RestAreaMapAvailable("https://abc.mapbox.com&")
        val action = RestAreaAction.CheckRestAreaMapAvailability(bannerInstructions)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapAvailable

        assertEquals(expected.sapaMapUrl, result.sapaMapUrl)
    }

    @Test
    fun `process prepare sapa map request action should return sapa map request`() {
        val action = RestAreaAction.PrepareRestAreaMapRequest("https://abc.mapbox.com")

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapRequest

        assertEquals(action.sapaMapUrl, result.request.url)
    }

    @Test
    fun `process process sapa map response action with resource load status unauthorized`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.UNAUTHORIZED,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)
        val expected = RestAreaResult.RestAreaMapSvg.Failure(
            "Your token cannot access this resource, contact support"
        )

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process process sapa map response action with resource load status not found`() {
        val loadResult = resourceLoadResult(
            data = null,
            status = ResourceLoadStatus.NOT_FOUND,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = RestAreaResult.RestAreaMapSvg.Failure("Resource is missing")
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process sapa map response action with resource load status available without data`() {
        val loadResult = resourceLoadResult(
            data = resourceData(ByteArray(0)),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = RestAreaResult.RestAreaMapSvg.Empty
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Empty

        assertEquals(expected, result)
    }

    @Test
    fun `process sapa map response action with resource load status available`() {
        val blob = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val loadResult = resourceLoadResult(
            data = resourceData(blob),
            status = ResourceLoadStatus.AVAILABLE,
        )
        val response: Expected<ResourceLoadError, ResourceLoadResult> = createValue(loadResult)
        val expected = RestAreaResult.RestAreaMapSvg.Success(blob)
        val action = RestAreaAction.ProcessRestAreaMapResponse(response)

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaMapSvg.Success

        assertEquals(expected.data, result.data)
    }

    @Test
    fun `process action sapa map process bytearray to bitmap failure`() {
        mockkObject(SvgUtil)
        val mockData = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val mockOptions = mockk<MapboxRestAreaOptions> {
            every { desiredGuideMapWidth } returns 1000
        }
        val action = RestAreaAction.ParseSvgToBitmap(mockData, mockOptions)
        every {
            SvgUtil.renderAsBitmapWithWidth(any(), any(), any())
        }.throws(IllegalStateException("whatever"))

        val result = RestAreaProcessor.process(action) as RestAreaResult.RestAreaBitmap.Failure

        assertEquals("whatever", result.error)
        unmockkObject(SvgUtil)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `process CheckUpcomingRestStop should return SAPA map url if upcoming rest stop is on current step`() {
        // SETUP:
        //                         (current progress)    (upcoming rest stop)
        //                                 |                     |
        //                   (step 2)      |                     |      (step 3)
        //                       |---------x---------------------R---------|>
        // step dist. remaining            {           1200m               }
        // dist. to rest stop              {      1000m          }

        val restAreaLocation = Point.fromLngLat(1.0, 2.0)
        val restAreaMapUri = "http://example.com/rest-area-map/1.png"
        val nativeRestAreaObject = nativeRestAreaObjectWith(
            name = "rest-area-1",
            location = restAreaLocation,
            mapUri = restAreaMapUri
        )

        val routeProgress = routeProgressWith(
            currentStepDistanceRemaining = 1200f,
            upcomingRoadObjects = listOf<UpcomingRoadObject>(
                buildUpcomingRoadObject(
                    roadObject = buildRoadObject(nativeRestAreaObject),
                    distanceToStart = 1000.0,
                    distanceInfo = null
                )
            )
        )

        val result = RestAreaProcessor.process(
            RestAreaAction.CheckUpcomingRestStop(routeProgress)
        ) as? RestAreaResult.RestAreaMapAvailable

        assertNotNull("expected RestAreaResult.RestAreaMapAvailable", result)
        assertEquals(restAreaMapUri, result?.sapaMapUrl)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `process CheckUpcomingRestStop should NOT return SAPA map url if upcoming rest stop is NOT on current step`() {
        // SETUP:
        //                         (current progress)    (upcoming rest stop)
        //                                 |                     |
        //                   (step 1)      |    (step 2)         |      (step 3)
        //                       |---------x------|--------------R---------|>
        // step dist. remaining            { 200m }
        // dist. to rest stop              {        1200m        }

        val restAreaLocation = Point.fromLngLat(1.0, 2.0)
        val restAreaMapUri = "http://example.com/rest-area-map/1.png"
        val nativeRestAreaObject = nativeRestAreaObjectWith(
            name = "rest-area-1",
            location = restAreaLocation,
            mapUri = restAreaMapUri
        )

        val routeProgress = routeProgressWith(
            currentStepDistanceRemaining = 200f,
            upcomingRoadObjects = listOf<UpcomingRoadObject>(
                buildUpcomingRoadObject(
                    roadObject = buildRoadObject(nativeRestAreaObject),
                    distanceToStart = 1200.0,
                    distanceInfo = null
                )
            )
        )

        val result = RestAreaProcessor.process(
            RestAreaAction.CheckUpcomingRestStop(routeProgress)
        )

        assertTrue(
            "expected RestAreaResult.RestAreaMapUnavailable",
            result is RestAreaResult.RestAreaMapUnavailable
        )
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun routeProgressWith(
        currentStepDistanceRemaining: Float,
        upcomingRoadObjects: List<UpcomingRoadObject>
    ): RouteProgress = buildRouteProgressObject(
        route = mockk(),
        bannerInstructions = null,
        voiceInstructions = null,
        currentState = RouteProgressState.TRACKING,
        currentLegProgress = mockk<RouteLegProgress> {
            every { legIndex } returns 0
            every { routeLeg } returns null
            every { distanceTraveled } returns 0f
            every { distanceRemaining } returns 100f
            every { durationRemaining } returns 123.0
            every { fractionTraveled } returns 0.1f
            every { currentStepProgress } returns mockk<RouteStepProgress> {
                every { step } returns mockk()
                every { distanceRemaining } returns currentStepDistanceRemaining
            }
            every { upcomingStep } returns null
            every { geometryIndex } returns 0
        },
        upcomingStepPoints = null,
        inTunnel = false,
        distanceRemaining = 100f,
        distanceTraveled = 10f,
        durationRemaining = 123.0,
        fractionTraveled = 0.1f,
        remainingWaypoints = 0,
        upcomingRoadObjects = upcomingRoadObjects,
        stale = false,
        alternativeRouteId = null,
        currentRouteGeometryIndex = 0,
        alternativeRoutesIndices = emptyMap(),
    )

    private fun nativeRestAreaObjectWith(name: String, location: Point, mapUri: String) =
        RoadObject(
            name,
            0.0,
            MatchedRoadObjectLocation(
                NativeStub.MatchedPointLocation(
                    Position(
                        GraphPosition(1, 0.0),
                        location
                    )
                )
            ),
            RoadObjectType.SERVICE_AREA,
            RoadObjectProvider.MAPBOX,
            RoadObjectMetadata(
                ServiceAreaInfo(
                    "id#0",
                    ServiceAreaType.REST_AREA,
                    name,
                    listOf(
                        Amenity(AmenityType.GAS_STATION, "Get GAS", "FuelItUp")
                    ),
                    mapUri
                )
            ),
            true
        )

    private fun getComponentGuidanceViewType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSapaSubType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SAPAGUIDEMAP)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSapaSubTypeImageUrl(url: String): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SAPAGUIDEMAP)
            .text("some text")
            .imageUrl(url)
            .build()
    }

    private fun resourceData(blob: ByteArray) = object : ResourceData(0) {
        override fun getData(): ByteArray = blob
    }

    private fun resourceLoadResult(
        data: ResourceData?,
        status: ResourceLoadStatus,
        immutable: Boolean = false,
        mustRevalidate: Boolean = false,
        expires: Date = Date(),
        totalBytes: Long = 0,
        transferredBytes: Long = 0,
        contentType: String = "image/png"
    ): ResourceLoadResult {
        return ResourceLoadResult(
            data,
            status,
            immutable,
            mustRevalidate,
            expires,
            totalBytes,
            transferredBytes,
            contentType
        )
    }

    object NativeStub {
        class MatchedPointLocation(
            private val pos: Position
        ) : com.mapbox.navigator.MatchedPointLocation(0) {
            override fun getPosition(): Position = pos
        }
    }
}
