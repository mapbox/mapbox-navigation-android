package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterFactory
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.NavigationComponentProvider
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
    val routeParserRule = NativeRouteParserRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    val router = mockk<NavigationRouterV2>(relaxed = true)
    private val routesProgressDataProvider = RoutesProgressDataProvider()
    private val directionsSession = NavigationComponentProvider.createDirectionsSession(router)
    lateinit var routeRefreshController: RouteRefreshController
    val stateObserver = TestStateObserver()
    val refreshObserver = TestRefreshObserver()
    val testDispatcher = coroutineRule.testDispatcher
    private val routeRefreshAnswerScope = coroutineRule.createTestScope()

    class TestRefreshObserver : RouteRefreshObserver {

        val refreshes = mutableListOf<RoutesRefresherResult>()

        override fun onRoutesRefreshed(routeInfo: RoutesRefresherResult) {
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
        mockkObject(CoroutineUtils)
        every {
            CoroutineUtils.createScope(any(), any())
        } answers { coroutineRule.createTestScope(firstArg<Job>()) }

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
        routeRefreshAnswerScope.cancel()
        routeRefreshController.destroy()
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

    @OptIn(ExperimentalMapboxNavigationAPI::class)
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
            routeRefreshAnswerScope.launch {
                delay(responseDelay)
                if (invocationNumber >= successfulAttemptNumber) {
                    callback.onRefreshReady(refreshedRoute)
                } else {
                    callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
                }
            }
            invocationNumber++
            0
        }
    }

    fun updateProgressWithGeometryIndex(index: Int) {
        routesProgressDataProvider.onRouteProgressChanged(
            mockk {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { geometryIndex } returns 0
                }
                every { currentRouteGeometryIndex } returns index
                every { internalAlternativeRouteIndices() } returns emptyMap()
            }
        )
    }
}
