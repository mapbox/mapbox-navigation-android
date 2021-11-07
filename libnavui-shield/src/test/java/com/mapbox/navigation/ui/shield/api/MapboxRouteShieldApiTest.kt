package com.mapbox.navigation.ui.shield.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.shield.RoadShieldContentManagerContainer
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldOrigin
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxRouteShieldApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val routeShieldApi = MapboxRouteShieldApi()

    @Before
    fun setUp() {
        mockkObject(RoadShieldContentManagerContainer)
    }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldContentManagerContainer)
    }

    @Test
    fun `when get shields for all empty legacy urls`() {
        val bannerComponents: BannerComponents = mockk(relaxed = true)
        val bannerComponentsList = listOf(bannerComponents)
        val routeShieldToDownload = emptyList<RouteShieldToDownload>()
        coEvery {
            RoadShieldContentManagerContainer.getShields(routeShieldToDownload)
        } returns emptyList()
        val callback: RouteShieldCallback = mockk(relaxed = true)
        val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

        routeShieldApi.getRouteShields(bannerComponentsList, callback)

        verify(exactly = 1) {
            callback.onRoadShields(capture(shieldSlot))
        }
    }

    @Test
    fun `when get shields for all legacy urls with all shields`() =
        coroutineRule.runBlockingTest {
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1)
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = ""
                        )
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(bannerComponentsList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
        }

    @Test
    fun `when get shields for all legacy urls with shields and errors`() =
        coroutineRule.runBlockingTest {
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1)
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = ""
                        )
                    )
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2",
                        errorMessage = "whatever"
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(bannerComponentsList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertNotNull(shieldSlot.captured[0].value)
            assertNotNull(shieldSlot.captured[1].error)
        }

    @Test
    fun `when get shields for all legacy urls with all errors`() =
        coroutineRule.runBlockingTest {
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url1",
                        errorMessage = "whatever"
                    )
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2",
                        errorMessage = "whatever"
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(bannerComponentsList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
        }

    @Test
    fun `when get shields for all empty mapbox designed urls with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val accessToken = "pk.123"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockk(),
                            shieldSprite = mockk()
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = ""
                        )
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                accessToken,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
        }

    @Test
    fun `when get shields for all empty mapbox designed urls with shields and errors`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val accessToken = "pk.123"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockk(),
                            shieldSprite = mockk()
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = ""
                        )
                    )
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2",
                        errorMessage = "whatever"
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                accessToken,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertNotNull(shieldSlot.captured[0].value)
            assertNotNull(shieldSlot.captured[1].error)
        }

    @Test
    fun `when get shields for all mapbox designed urls with all errors`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val accessToken = "pk.123"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url1",
                        errorMessage = "whatever"
                    )
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2",
                        errorMessage = "whatever"
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                accessToken,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
        }

    @Test
    fun `when get shields for mapbox designed and legacy combined with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val accessToken = "pk.123"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockk(),
                            shieldSprite = mockk()
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = ""
                        )
                    )
                ),
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = "https://shield.mapbox.com/legacy/url1",
                            byteArray = byteArrayOf(1)
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = true,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = "placeholder was empty"
                        )
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                accessToken,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
            assertTrue(shieldSlot.captured[1].value!!.origin.isFallback)
        }

    @Test
    fun `when get mapbox shields using road with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val accessToken = "pk.123"
            val mockMapboxShield1 = MapboxShield
                .builder()
                .name("us-interstate")
                .baseUrl("https://shields.mapbox.com/mapbox/designed/using/road/1")
                .textColor("black")
                .displayRef("880")
                .build()
            val mockMapboxShield2 = MapboxShield
                .builder()
                .name("us-interstate")
                .baseUrl("https://shields.mapbox.com/mapbox/designed/using/road/2")
                .textColor("black")
                .displayRef("680")
                .build()
            val road = mockk<Road> {
                every { name } returns "Central Av"
                every { shieldName } returns "I 880"
                every { shieldUrl } returns null
                every { mapboxShield } returns listOf(mockMapboxShield1, mockMapboxShield2)
            }
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockMapboxShield1,
                            shieldSprite = mockk()
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            originalErrorMessage = ""
                        )
                    )
                ),
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shields.mapbox.com/mapbox/designed/using/road/2",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockMapboxShield1,
                            shieldSprite = mockk()
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/2",
                            originalErrorMessage = ""
                        )
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                road,
                userId,
                styleId,
                accessToken,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
            assertEquals(
                mockResultList[1].value!!.shield.url,
                shieldSlot.captured[1].value!!.shield.url
            )
        }

    @Test
    fun `when get legacy shields using road with all shields`() =
        coroutineRule.runBlockingTest {
            val mockLegacyUrl1 = "https://shields.mapbox.com/mapbox/legacy/using/road/1"
            val road = mockk<Road> {
                every { name } returns "Central Av"
                every { shieldName } returns "I 880"
                every { shieldUrl } returns mockLegacyUrl1
                every { mapboxShield } returns null
            }
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = "https://shields.mapbox.com/mapbox/legacy/using/road/1",
                            byteArray = byteArrayOf(1)
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            originalErrorMessage = ""
                        )
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                road,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
            assertEquals(
                mockResultList[0].value!!.shield.url,
                shieldSlot.captured[0].value!!.shield.url
            )
        }

    @Test
    fun `when get legacy and mapbox shields using road with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val accessToken = "pk.123"
            val mockMapboxShield1 = MapboxShield
                .builder()
                .name("us-interstate")
                .baseUrl("https://shields.mapbox.com/mapbox/designed/using/road/1")
                .textColor("black")
                .displayRef("880")
                .build()
            val mockLegacyUrl1 = "https://shields.mapbox.com/mapbox/legacy/using/road/1"
            val road = mockk<Road> {
                every { name } returns "Central Av"
                every { shieldName } returns "I 880"
                every { shieldUrl } returns mockLegacyUrl1
                every { mapboxShield } returns listOf(mockMapboxShield1)
            }
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = "https://shields.mapbox.com/mapbox/legacy/using/road/1",
                            byteArray = byteArrayOf(1)
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            originalErrorMessage = ""
                        )
                    )
                ),
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockMapboxShield1,
                            shieldSprite = mockk()
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            originalErrorMessage = ""
                        )
                    )
                )
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                road,
                userId,
                styleId,
                accessToken,
                callback
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertEquals(
                mockResultList[0].value!!.shield.url,
                shieldSlot.captured[0].value!!.shield.url
            )
            assertEquals(
                mockResultList[1].value!!.shield.url,
                shieldSlot.captured[1].value!!.shield.url
            )
        }
}
