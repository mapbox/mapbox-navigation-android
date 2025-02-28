package com.mapbox.navigation.tripdata.shield.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.shield.RoadShieldContentManagerContainer
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldOrigin
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns "pk.1234"
    }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldContentManagerContainer)
        unmockkStatic(MapboxOptions::class)
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
            val initialUrl1 = "https://shield.mapbox.com/url1"
            val toDownloadUrl1 = "https://shield.mapbox.com/url1.svg"
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = toDownloadUrl1,
                            byteArray = byteArrayOf(1),
                            initialUrl = initialUrl1,
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = toDownloadUrl1,
                            originalErrorMessage = "",
                        ),
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(bannerComponentsList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
        }

    @Test
    fun `when get shields for all legacy urls with shields and errors`() =
        coroutineRule.runBlockingTest {
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val initialUrl1 = "https://shield.mapbox.com/url1"
            val toDownloadUrl1 = "https://shield.mapbox.com/url1.svg"
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = toDownloadUrl1,
                            byteArray = byteArrayOf(1),
                            initialUrl = initialUrl1,
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = toDownloadUrl1,
                            originalErrorMessage = "",
                        ),
                    ),
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2.svg",
                        errorMessage = "whatever",
                    ),
                ),
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
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
            assertEquals(mockResultList[1].error, shieldSlot.captured[1].error)
        }

    @Test
    fun `when get shields for all legacy urls with all errors`() =
        coroutineRule.runBlockingTest {
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val initialUrl1 = "https://shield.mapbox.com/url1"
            val initialUrl2 = "https://shield.mapbox.com/url2"
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = initialUrl1.plus(".svg"),
                        errorMessage = "whatever",
                    ),
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = initialUrl2.plus(".svg"),
                        errorMessage = "whatever",
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(bannerComponentsList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertEquals(mockResultList[0].error, shieldSlot.captured[0].error)
            assertEquals(mockResultList[1].error, shieldSlot.captured[1].error)
        }

    @Test
    fun `when get shields for all empty mapbox designed urls with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockk(),
                            shieldSprite = mockk(),
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = "",
                        ),
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
        }

    @Test
    fun `when get shields for all empty mapbox designed urls with shields and errors`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockk(),
                            shieldSprite = mockk(),
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = "",
                        ),
                    ),
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2",
                        errorMessage = "whatever",
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertNotNull(shieldSlot.captured[0].value)
            assertNotNull(shieldSlot.captured[1].error)
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
            assertEquals(mockResultList[1].error, shieldSlot.captured[1].error)
        }

    @Test
    fun `when get shields for all mapbox designed urls with all errors`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url1",
                        errorMessage = "whatever",
                    ),
                ),
                ExpectedFactory.createError(
                    RouteShieldError(
                        url = "https://shield.mapbox.com/url2",
                        errorMessage = "whatever",
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertEquals(mockResultList[0].error, shieldSlot.captured[0].error)
            assertEquals(mockResultList[1].error, shieldSlot.captured[1].error)
        }

    @Test
    fun `when get shields for mapbox designed and legacy combined with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val bannerComponents: BannerComponents = mockk(relaxed = true)
            val bannerComponentsList = listOf(bannerComponents)
            val initialUrl1 = "https://shield.mapbox.com/legacy/url1"
            val toDownloadUrl1 = "https://shield.mapbox.com/legacy/url1.svg"
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shield.mapbox.com/url1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockk(),
                            shieldSprite = mockk(),
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shield.mapbox.com/url1",
                            originalErrorMessage = "",
                        ),
                    ),
                ),
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = toDownloadUrl1,
                            byteArray = byteArrayOf(1),
                            initialUrl = initialUrl1,
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = true,
                            originalUrl = toDownloadUrl1,
                            originalErrorMessage = "",
                        ),
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                bannerComponentsList,
                userId,
                styleId,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
            assertTrue(shieldSlot.captured[1].value!!.origin.isFallback)
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
            assertEquals(mockResultList[1].value, shieldSlot.captured[1].value)
        }

    @Test
    fun `when get mapbox shields using road with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
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
            val roadComponent1 = mockk<RoadComponent> {
                every { text } returns "Central Av"
                every { shield } returns mockMapboxShield1
                every { imageBaseUrl } returns null
            }
            val roadComponent2 = mockk<RoadComponent> {
                every { text } returns "North Av"
                every { shield } returns mockMapboxShield2
                every { imageBaseUrl } returns null
            }
            val road = mockk<Road> {
                every { components } returns listOf(roadComponent1, roadComponent2)
            }
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockMapboxShield1,
                            shieldSprite = mockk(),
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            originalErrorMessage = "",
                        ),
                    ),
                ),
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shields.mapbox.com/mapbox/designed/using/road/2",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockMapboxShield2,
                            shieldSprite = mockk(),
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/2",
                            originalErrorMessage = "",
                        ),
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                road,
                userId,
                styleId,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
            assertEquals(mockResultList[1].value, shieldSlot.captured[1].value)
        }

    @Test
    fun `get shields for road uses up-to-date access token`() = coroutineRule.runBlockingTest {
        val road = mockk<Road> {
            every { components } returns listOf(mockk(relaxed = true))
        }
        val userId = "userId"
        val styleId = "styleId"
        coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns emptyList()
        val callback: RouteShieldCallback = mockk(relaxed = true)

        routeShieldApi.getRouteShields(
            road,
            userId,
            styleId,
            callback,
        )

        val shieldsToDownloadSlot = slot<List<RouteShieldToDownload>>()
        coVerify {
            RoadShieldContentManagerContainer.getShields(
                capture(shieldsToDownloadSlot),
            )
        }
        shieldsToDownloadSlot.captured.forEach {
            val url = when (it) {
                is RouteShieldToDownload.MapboxDesign -> {
                    it.generateUrl(mockk(relaxed = true))
                }
                is RouteShieldToDownload.MapboxLegacy -> {
                    it.url
                }
            }
            assertTrue(url.contains("access_token=pk.1234"))
        }
        clearAllMocks(answers = false)

        every { MapboxOptions.accessToken } returns "new.token"
        routeShieldApi.getRouteShields(
            road,
            userId,
            styleId,
            callback,
        )

        val newShieldsToDownloadSlot = slot<List<RouteShieldToDownload>>()
        coVerify {
            RoadShieldContentManagerContainer.getShields(
                capture(newShieldsToDownloadSlot),
            )
        }
        newShieldsToDownloadSlot.captured.forEach {
            val url = when (it) {
                is RouteShieldToDownload.MapboxDesign -> {
                    it.generateUrl(mockk(relaxed = true))
                }
                is RouteShieldToDownload.MapboxLegacy -> {
                    it.url
                }
            }
            assertTrue(url.contains("access_token=new.token"))
        }
    }

    @Test
    fun `when get legacy shields using road with all shields`() =
        coroutineRule.runBlockingTest {
            val initialUrl1 = "https://shields.mapbox.com/mapbox/legacy/using/road/1"
            val toDownloadUrl1 = "https://shields.mapbox.com/mapbox/legacy/using/road/1.svg"
            val roadComponent = mockk<RoadComponent> {
                every { text } returns "Central Av"
                every { shield } returns null
                every { imageBaseUrl } returns initialUrl1
            }
            val road = mockk<Road> {
                every { components } returns listOf(roadComponent)
            }
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = toDownloadUrl1,
                            byteArray = byteArrayOf(1),
                            initialUrl = initialUrl1,
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = toDownloadUrl1,
                            originalErrorMessage = "",
                        ),
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                road,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
        }

    @Test
    fun `when get legacy and mapbox shields using road with all shields`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            val mockMapboxShield1 = MapboxShield
                .builder()
                .name("us-interstate")
                .baseUrl("https://shields.mapbox.com/mapbox/designed/using/road/1")
                .textColor("black")
                .displayRef("880")
                .build()
            val initialUrl1 = "https://shields.mapbox.com/mapbox/legacy/using/road/1"
            val toDownloadUrl1 = "https://shields.mapbox.com/mapbox/legacy/using/road/1.svg"
            val roadComponent = mockk<RoadComponent> {
                every { text } returns "Central Av"
                every { shield } returns mockMapboxShield1
                every { imageBaseUrl } returns initialUrl1
            }
            val road = mockk<Road> {
                every { components } returns listOf(roadComponent)
            }
            val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxLegacyShield(
                            url = toDownloadUrl1,
                            byteArray = byteArrayOf(1),
                            initialUrl = initialUrl1,
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = toDownloadUrl1,
                            originalErrorMessage = "",
                        ),
                    ),
                ),
                ExpectedFactory.createValue(
                    RouteShieldResult(
                        shield = RouteShield.MapboxDesignedShield(
                            url = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            byteArray = byteArrayOf(1),
                            mapboxShield = mockMapboxShield1,
                            shieldSprite = mockk(),
                        ),
                        origin = RouteShieldOrigin(
                            isFallback = false,
                            originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                            originalErrorMessage = "",
                        ),
                    ),
                ),
            )
            coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            routeShieldApi.getRouteShields(
                road,
                userId,
                styleId,
                callback,
            )

            verify(exactly = 1) {
                callback.onRoadShields(capture(shieldSlot))
            }
            assertEquals(mockResultList.size, shieldSlot.captured.size)
            assertEquals(mockResultList[0].value, shieldSlot.captured[0].value)
            assertEquals(mockResultList[1].value, shieldSlot.captured[1].value)
        }

    @Test
    fun `when get mapbox shields using road components`() = coroutineRule.runBlockingTest {
        val userId = "userId"
        val styleId = "styleId"
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
        val roadComponent1 = mockk<RoadComponent> {
            every { text } returns "Central Av"
            every { shield } returns mockMapboxShield1
            every { imageBaseUrl } returns null
        }
        val roadComponent2 = mockk<RoadComponent> {
            every { text } returns "North Av"
            every { shield } returns mockMapboxShield2
            every { imageBaseUrl } returns null
        }

        val mockResultList = listOf<Expected<RouteShieldError, RouteShieldResult>>(
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxDesignedShield(
                        url = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                        byteArray = byteArrayOf(1),
                        mapboxShield = mockMapboxShield1,
                        shieldSprite = mockk(),
                    ),
                    origin = RouteShieldOrigin(
                        isFallback = false,
                        originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/1",
                        originalErrorMessage = "",
                    ),
                ),
            ),
            ExpectedFactory.createValue(
                RouteShieldResult(
                    shield = RouteShield.MapboxDesignedShield(
                        url = "https://shields.mapbox.com/mapbox/designed/using/road/2",
                        byteArray = byteArrayOf(1),
                        mapboxShield = mockMapboxShield2,
                        shieldSprite = mockk(),
                    ),
                    origin = RouteShieldOrigin(
                        isFallback = false,
                        originalUrl = "https://shields.mapbox.com/mapbox/designed/using/road/2",
                        originalErrorMessage = "",
                    ),
                ),
            ),
        )
        coEvery { RoadShieldContentManagerContainer.getShields(any()) } returns mockResultList
        val callback: RouteShieldCallback = mockk(relaxed = true)
        val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

        routeShieldApi.getRoadComponentsShields(
            listOf(roadComponent1, roadComponent2),
            userId,
            styleId,
            callback,
        )

        verify(exactly = 1) {
            callback.onRoadShields(capture(shieldSlot))
        }
        assertEquals(mockResultList.size, shieldSlot.captured.size)
        assertFalse(shieldSlot.captured[0].value!!.origin.isFallback)
        assertEquals(mockResultList[1].value, shieldSlot.captured[1].value)
    }
}
