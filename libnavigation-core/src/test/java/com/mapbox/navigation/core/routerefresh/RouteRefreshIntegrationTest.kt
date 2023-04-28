package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.NavigationComponentProvider
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.RoutesRefreshData
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal open class RouteRefreshIntegrationTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mapboxReplayer = MapboxReplayer()
    private val threadController = ThreadController()
    val router = mockk<NavigationRouterV2>(relaxed = true)
    private val routesProgressDataProvider = RoutesProgressDataProvider()
    private val tripSession = NavigationComponentProvider.createTripSession(
        tripService = mockk(relaxed = true),
        TripSessionLocationEngine(mockk()) { ReplayLocationEngine(mapboxReplayer) },
        mockk(relaxed = true),
        threadController
    )
    private val directionsSession = NavigationComponentProvider.createDirectionsSession(router)
    private val routesAlternativeController = RouteAlternativesControllerProvider.create(
        RouteAlternativesOptions.Builder().build(),
        mockk(relaxed = true),
        tripSession,
        threadController
    )
    lateinit var routeRefreshController: RouteRefreshController
    val stateObserver = TestStateObserver()
    val refreshObserver = TestRefreshObserver()
    val testDispatcher = coroutineRule.testDispatcher
    val testScope = coroutineRule.createTestScope()

    class TestRefreshObserver : RouteRefreshObserver {

        val refreshes = mutableListOf<RoutesRefreshData>()

        override fun onRoutesRefreshed(routeInfo: RoutesRefreshData) {
            refreshes.add(routeInfo)
        }
    }

    class TestStateObserver : RouteRefreshStatesObserver {

        private val states = mutableListOf<RouteRefreshStateResult>()

        override fun onNewState(result: RouteRefreshStateResult) {
            states.add(result)
        }

        fun getStatesSnapshot(): List<String> = states.map { it.state }
    }

    @Before
    fun setUp() {
        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } returns ExpectedFactory.createValue(listOf(mockk(relaxed = true)))
        mockkObject(CoroutineUtils)
        every {
            CoroutineUtils.createScope(any(), any())
        } answers { coroutineRule.createTestScope() }

        routesProgressDataProvider.onRouteProgressChanged(
            mockk {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { geometryIndex } returns 0
                }
                every { currentRouteGeometryIndex } returns 0
                every { internalAlternativeRouteIndices() } returns emptyMap()
            }
        )
    }

    @After
    fun tearDown() {
        unmockkObject(NativeRouteParserWrapper)
        unmockkObject(CoroutineUtils)
    }

    fun createRefreshController(refreshInterval: Long): RouteRefreshController {
        val options = RouteRefreshOptions.Builder().intervalMillis(refreshInterval).build()
        return RouteRefreshControllerProvider.createRouteRefreshController(
            testDispatcher,
            testDispatcher,
            options,
            directionsSession,
            routesProgressDataProvider,
            EVDynamicDataHolder(),
            object : Time {
                override fun nanoTime() = System.nanoTime()

                override fun millis() = testDispatcher.currentTime

                override fun seconds() = millis() / 1000
            }
        )
    }

    fun setUpRoutes(
        fileName: String,
        enableRefresh: Boolean = true,
        successfulAttemptNumber: Int = 0,
        responseDelay: Long = 0
    ): List<NavigationRoute> {
        setUpRouteRefresh(fileName, successfulAttemptNumber, responseDelay)
        return NavigationRoute.create(
            DirectionsResponse.fromJson(FileUtils.loadJsonFixture(fileName)),
            RouteOptions.builder()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(-121.496066, 38.577764),
                        Point.fromLngLat(-121.480256, 38.576795)
                    )
                )
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .enableRefresh(enableRefresh)
                .build(),
            RouterOrigin.Custom()
        )
    }

    fun failRouteRefreshResponse() {
        every { router.getRouteRefresh(any(), any<RouteRefreshRequestData>(), any()) } answers {
            val callback = thirdArg() as NavigationRouterRefreshCallback
            callback.onFailure(mockk(relaxed = true))
            0
        }
    }

    fun setUpRouteRefresh(
        fileName: String,
        successfulAttemptNumber: Int = 0,
        responseDelay: Long = 0
    ) {
        val refreshedRoute = NavigationRoute.create(
            DirectionsResponse.fromJson(FileUtils.loadJsonFixture(fileName)),
            RouteOptions.builder()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(-121.496066, 38.577764),
                        Point.fromLngLat(-121.480256, 38.576795)
                    )
                )
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .enableRefresh(true)
                .build(),
            RouterOrigin.Custom()
        ).first()
        var invocationNumber = 0
        every { router.getRouteRefresh(any(), any<RouteRefreshRequestData>(), any()) } answers {
            val callback = thirdArg() as NavigationRouterRefreshCallback
            refreshedRoute.update(
                {
                    toBuilder()
                        .legs(
                            this.legs()!!.map { leg ->
                                leg.toBuilder()
                                    .annotation(
                                        leg.annotation()!!.toBuilder()
                                            .duration(
                                                leg.annotation()!!.duration()!!.map {
                                                    it + (invocationNumber + 1) * 0.1
                                                }
                                            )
                                            .build()
                                    )
                                    .build()
                            }
                        )
                        .build()
                },
                { this }
            )
            testScope.launch {
                delay(responseDelay)
                if (invocationNumber >= successfulAttemptNumber) {
                    callback.onRefreshReady(refreshedRoute)
                } else {
                    callback.onFailure(mockk(relaxed = true))
                }
            }
            invocationNumber++
            0
        }
    }
}
