package com.mapbox.navigation.ui.androidauto.navigation

import android.content.Context
import android.graphics.Bitmap
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import androidx.car.app.navigation.model.RoutingInfo
import androidx.core.graphics.drawable.IconCompat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.LaneFactory
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuverFactory
import com.mapbox.navigation.ui.androidauto.navigation.lanes.CarLanesImage
import com.mapbox.navigation.ui.androidauto.navigation.lanes.CarLanesImageRenderer
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class CarNavigationInfoMapperTest {

    private lateinit var instructionRenderer: CarManeuverInstructionRenderer
    private lateinit var iconRenderer: CarManeuverIconRenderer
    private lateinit var imageGenerator: CarLanesImageRenderer
    private lateinit var sut: CarNavigationInfoMapper

    @Before
    fun setup() {
        mockkStatic(CarDistanceFormatter::class)
        every { CarDistanceFormatter.carDistance(any()) } answers {
            Distance.create(firstArg(), Distance.UNIT_METERS)
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        instructionRenderer = mockk(relaxed = true)
        iconRenderer = mockk(relaxed = true)
        imageGenerator = mockk(relaxed = true)

        sut = CarNavigationInfoMapper(
            context,
            instructionRenderer,
            iconRenderer,
            imageGenerator,
        )
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `mapNavigationInfo - should return NULL when distanceRemaining data is not available`() {
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { currentStepProgress } returns null
            }
        }

        val result = sut.mapNavigationInfo(
            expectedManeuvers = ExpectedFactory.createError(mockk()),
            routeShields = emptyList(),
            routeProgress = routeProgress,
            junctionValue = null,
        )

        assertNull(result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `mapNavigationInfo - should return RoutingInfo with Maneuver info`() {
        val renderedPrimaryInstruction = "rendered primary maneuver instruction"
        val renderedSecondaryInstruction = "rendered secondary maneuver instruction"
        given(
            renderedPrimaryInstruction = renderedPrimaryInstruction,
            renderedSecondaryInstruction = renderedSecondaryInstruction,
        )

        val result = sut.mapNavigationInfo(
            expectedManeuvers = ExpectedFactory.createValue(listOf(TEST_MANEUVER)),
            routeShields = emptyList(),
            routeProgress = TEST_ROUTE_PROGRESS,
            junctionValue = null,
        ) as RoutingInfo

        val step = result.currentStep
        assertNotNull(step)
        assertEquals(
            CarText.create(
                "$renderedPrimaryInstruction\n$renderedSecondaryInstruction",
            ).toCharSequence(),
            step!!.cue!!.toCharSequence(),
        )
        assertEquals(
            androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_RIGHT,
            step.maneuver!!.type,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `mapNavigationInfo - should return RoutingInfo with lane guidance info`() {
        val renderedLanesImage = CarLanesImage(
            listOf(
                Lane.Builder()
                    .addDirection(LaneDirection.create(LaneDirection.SHAPE_STRAIGHT, false))
                    .addDirection(LaneDirection.create(LaneDirection.SHAPE_NORMAL_RIGHT, true))
                    .build(),
            ),
            CarIcon.Builder(IconCompat.createWithBitmap(sampleBitmap())).build(),
        )
        given(
            renderedPrimaryInstruction = "rendered primary maneuver instruction",
            renderedSecondaryInstruction = "rendered secondary maneuver instruction",
            renderedLanesImage = renderedLanesImage,
        )

        val result = sut.mapNavigationInfo(
            expectedManeuvers = ExpectedFactory.createValue(listOf(TEST_MANEUVER)),
            routeShields = emptyList(),
            routeProgress = TEST_ROUTE_PROGRESS,
            junctionValue = null,
        ) as RoutingInfo

        assertEquals(renderedLanesImage.carIcon, result.currentStep!!.lanesImage)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `mapNavigationInfo - should return RoutingInfo with an optional junction image`() {
        val junctionBitmap = sampleBitmap()
        val junctionValue = mockk<JunctionValue> {
            every { bitmap } returns junctionBitmap
        }
        given(
            renderedPrimaryInstruction = "rendered primary maneuver instruction",
            renderedSecondaryInstruction = "rendered secondary maneuver instruction",
        )

        val result = sut.mapNavigationInfo(
            expectedManeuvers = ExpectedFactory.createValue(listOf(TEST_MANEUVER)),
            routeShields = emptyList(),
            routeProgress = TEST_ROUTE_PROGRESS,
            junctionValue = junctionValue,
        ) as RoutingInfo

        assertEquals(
            CarIcon.Builder(IconCompat.createWithBitmap(junctionBitmap)).build(),
            result.junctionImage,
        )
    }

    @Test
    fun `mapNavigationInfo - should map primary roundabout maneuver using driving side and exit number`() {
        every { imageGenerator.renderLanesImage(any()) } returns null
        val roundaboutManeuver = ManeuverFactory.buildManeuver(
            primary = PrimaryManeuverFactory.buildPrimaryManeuver(
                id = "primary_roundabout",
                text = "Take the 3rd exit",
                type = StepManeuver.ROUNDABOUT,
                degrees = null,
                modifier = ManeuverModifier.RIGHT,
                drivingSide = "left",
                componentList = listOf(
                    Component(
                        type = BannerComponents.EXIT_NUMBER,
                        node = ExitNumberComponentNode.Builder().text("3").build(),
                    ),
                ),
            ),
            stepDistance = mockk(),
            secondary = null,
            sub = null,
            lane = null,
            point = Point.fromLngLat(10.0, 20.0),
        )

        val result = sut.mapNavigationInfo(
            expectedManeuvers = ExpectedFactory.createValue(listOf(roundaboutManeuver)),
            routeShields = emptyList(),
            routeProgress = TEST_ROUTE_PROGRESS,
            junctionValue = null,
        ) as RoutingInfo

        val carManeuver = result.currentStep!!.maneuver!!
        assertEquals(
            androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
            carManeuver.type,
        )
        assertEquals(3, carManeuver.roundaboutExitNumber)
    }

    @Test
    fun `mapNavigationInfo - should map next-step roundabout maneuver using driving side and exit number`() {
        every { imageGenerator.renderLanesImage(any()) } returns null
        val maneuverWithRoundaboutSub = ManeuverFactory.buildManeuver(
            primary = PrimaryManeuverFactory.buildPrimaryManeuver(
                id = "primary_turn",
                text = "Turn Right",
                type = StepManeuver.TURN,
                degrees = 0.0,
                modifier = ManeuverModifier.RIGHT,
                drivingSide = "left",
                componentList = emptyList(),
            ),
            stepDistance = mockk(),
            secondary = null,
            sub = SubManeuverFactory.buildSubManeuver(
                id = "sub_roundabout",
                text = "Then take the 2nd exit",
                type = StepManeuver.ROUNDABOUT,
                degrees = null,
                modifier = ManeuverModifier.RIGHT,
                drivingSide = "left",
                componentList = listOf(
                    Component(
                        type = BannerComponents.EXIT_NUMBER,
                        node = ExitNumberComponentNode.Builder().text("2").build(),
                    ),
                ),
            ),
            lane = null,
            point = Point.fromLngLat(10.0, 20.0),
        )

        val result = sut.mapNavigationInfo(
            expectedManeuvers = ExpectedFactory.createValue(listOf(maneuverWithRoundaboutSub)),
            routeShields = emptyList(),
            routeProgress = TEST_ROUTE_PROGRESS,
            junctionValue = null,
        ) as RoutingInfo

        val nextCarManeuver = result.nextStep!!.maneuver!!
        assertEquals(
            androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
            nextCarManeuver.type,
        )
        assertEquals(2, nextCarManeuver.roundaboutExitNumber)
    }

    private fun given(
        renderedPrimaryInstruction: String,
        renderedSecondaryInstruction: String,
        renderedLanesImage: CarLanesImage? = null,
    ) {
        every {
            instructionRenderer.renderInstruction(
                maneuver = TEST_MANEUVER.primary.componentList,
                shields = any(),
                exitView = any(),
                modifier = TEST_MANEUVER.primary.modifier,
                any(),
            )
        } returns renderedPrimaryInstruction
        every {
            instructionRenderer.renderInstruction(
                maneuver = TEST_MANEUVER.secondary!!.componentList,
                shields = any(),
                exitView = any(),
                modifier = TEST_MANEUVER.secondary!!.modifier,
                any(),
            )
        } returns renderedSecondaryInstruction
        every {
            imageGenerator.renderLanesImage(any())
        } returns renderedLanesImage
    }

    @Suppress("PrivatePropertyName")
    private val TEST_ROUTE_PROGRESS = mockk<RouteProgress> {
        every { currentLegProgress } returns mockk {
            every { currentStepProgress } returns mockk {
                every { distanceRemaining } returns 1000f
            }
        }
    }

    @Suppress("PrivatePropertyName")
    private val MANEUVER_COMPONENT_ROAD_SHIELD1: Component = Component(
        type = "",
        node = RoadShieldComponentNode.Builder()
            .shieldUrl("https://shield.mapbox.com/primary/url1")
            .text("")
            .mapboxShield(null)
            .build(),
    )

    @Suppress("PrivatePropertyName")
    private val MANEUVER_COMPONENT_ROAD_SHIELD2: Component = Component(
        type = "",
        node = RoadShieldComponentNode.Builder()
            .shieldUrl("https://shield.mapbox.com/primary/url2")
            .text("")
            .mapboxShield(null)
            .build(),
    )

    @Suppress("PrivatePropertyName")
    private val TEST_MANEUVER: Maneuver = ManeuverFactory.buildManeuver(
        primary = PrimaryManeuverFactory.buildPrimaryManeuver(
            id = "primary_0",
            text = "Turn Right",
            type = StepManeuver.TURN,
            degrees = 0.0,
            modifier = ManeuverModifier.RIGHT,
            drivingSide = "right",
            componentList = listOf(
                MANEUVER_COMPONENT_ROAD_SHIELD1,
                MANEUVER_COMPONENT_ROAD_SHIELD2,
            ),
        ),
        stepDistance = mockk(),
        secondary = SecondaryManeuverFactory.buildSecondaryManeuver(
            id = "secondary_0",
            text = "Continue Straight",
            type = StepManeuver.CONTINUE,
            degrees = 0.0,
            modifier = ManeuverModifier.STRAIGHT,
            drivingSide = "right",
            componentList = emptyList(),
        ),
        sub = null,
        lane = LaneFactory.buildLane(
            listOf(
                LaneIndicator.Builder().directions(listOf("straight")).isActive(false).build(),
                LaneIndicator.Builder().directions(listOf("right")).isActive(true).build(),
            ),
        ),
        point = Point.fromLngLat(10.0, 20.0),
    )

    private fun sampleBitmap() = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
}
