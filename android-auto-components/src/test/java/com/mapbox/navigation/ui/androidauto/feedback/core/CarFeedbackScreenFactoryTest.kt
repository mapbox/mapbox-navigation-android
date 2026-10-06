package com.mapbox.navigation.ui.androidauto.feedback.core

import android.graphics.Bitmap
import androidx.car.app.CarContext
import androidx.car.app.model.GridTemplate
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.ActiveGuidanceFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FavoritesFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FreeDriveFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.GeoDeeplinkPlacesFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.RoutePreviewFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.SearchPlacesFeedbackScreenFactory
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class CarFeedbackScreenFactoryTest : MapboxRobolectricTestRunner() {

    private val carContext: CarContext = mockk()
    private val bitmap: Bitmap = mockk(relaxUnitFun = true)

    // Delivered only by the tests that check encoding, so no other test starts background work.
    private var snapshotListener: MapView.OnSnapshotReady? = null
    private val mapSurface: MapSurface = mockk {
        every { snapshot(any<MapView.OnSnapshotReady>()) } answers {
            snapshotListener = firstArg()
        }
    }
    private val mapboxCarMap: MapboxCarMap = mockk {
        every { carMapSurface } returns mockk {
            every { mapSurface } returns this@CarFeedbackScreenFactoryTest.mapSurface
        }
    }
    private val poll = CarFeedbackPoll("Title", emptyList())
    private val pollProvider: CarFeedbackPollProvider = mockk {
        every { getFreeDriveFeedbackPoll(any()) } returns poll
        every { getActiveGuidanceFeedbackPoll(any()) } returns poll
        every { getRoutePreviewFeedbackPoll(any()) } returns poll
        every { getPlaceFeedbackPoll(any()) } returns poll
    }
    private val carOptions = MapboxCarOptions().apply {
        applyCustomization(
            MapboxCarOptions.Customization().apply { feedbackPollProvider = pollProvider },
        )
    }
    private val screenManager: MapboxScreenManager = mockk(relaxed = true)
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { carContext } returns this@CarFeedbackScreenFactoryTest.carContext
        every { mapboxCarMap } returns this@CarFeedbackScreenFactoryTest.mapboxCarMap
        every { options } returns carOptions
        every { mapboxScreenManager } returns screenManager
    }

    @Before
    fun setup() {
        mockkObject(FeedbackHelper)
        mockkStatic(FeedbackHelper::class)
        every { FeedbackHelper.encodeScreenshot(any(), any()) } returns "encoded"
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `create requests the map snapshot asynchronously`() {
        FreeDriveFeedbackScreenFactory(mapboxCarContext).create(carContext)

        verify(exactly = 1) { mapSurface.snapshot(any<MapView.OnSnapshotReady>()) }
        verify(exactly = 0) { mapSurface.snapshot() }
    }

    @Test
    fun `create encodes the map snapshot with the default options`() {
        FreeDriveFeedbackScreenFactory(mapboxCarContext).create(carContext)
        snapshotListener!!.onSnapshotReady(bitmap)

        verify(exactly = 1, timeout = TIMEOUT_MS) {
            FeedbackHelper.encodeScreenshot(
                bitmap,
                CarFeedbackOptions.Builder().build().bitmapEncodeOptions,
            )
        }
    }

    @Test
    fun `create encodes the map snapshot with customized options`() {
        val encodeOptions = BitmapEncodeOptions.Builder().compressQuality(90).width(1024).build()
        carOptions.applyCustomization(
            MapboxCarOptions.Customization().apply {
                carFeedbackOptions = CarFeedbackOptions.Builder()
                    .bitmapEncodeOptions(encodeOptions)
                    .build()
            },
        )

        FreeDriveFeedbackScreenFactory(mapboxCarContext).create(carContext)
        snapshotListener!!.onSnapshotReady(bitmap)

        verify(exactly = 1, timeout = TIMEOUT_MS) {
            FeedbackHelper.encodeScreenshot(bitmap, encodeOptions)
        }
    }

    @Test
    fun `create without a map surface skips the snapshot`() {
        every { mapboxCarMap.carMapSurface } returns null

        val screen = FreeDriveFeedbackScreenFactory(mapboxCarContext).create(carContext)

        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
        assertEquals("Title", (screen.onGetTemplate() as GridTemplate).title.toString())
    }

    @Test
    fun `create does not take a snapshot when screenshots are disabled`() {
        carOptions.applyCustomization(
            MapboxCarOptions.Customization().apply {
                carFeedbackOptions = CarFeedbackOptions.Builder().attachScreenshot(false).build()
            },
        )

        FreeDriveFeedbackScreenFactory(mapboxCarContext).create(carContext)

        verify(exactly = 0) { mapSurface.snapshot(any<MapView.OnSnapshotReady>()) }
        verify(exactly = 0) { mapSurface.snapshot() }
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `onFinish goes back by default`() {
        FreeDriveFeedbackScreenFactory(mapboxCarContext).onFinish()

        verify(exactly = 1) { screenManager.goBack() }
    }

    @Test
    fun `free drive factory uses the free drive poll`() {
        val factory = FreeDriveFeedbackScreenFactory(mapboxCarContext)

        assertSame(poll, factory.getCarFeedbackPoll(carContext))
        assertEquals(MapboxScreen.FREE_DRIVE, factory.getSourceName())
        verify(exactly = 1) { pollProvider.getFreeDriveFeedbackPoll(carContext) }
    }

    @Test
    fun `active guidance factory uses the active guidance poll`() {
        val factory = ActiveGuidanceFeedbackScreenFactory(mapboxCarContext)

        assertSame(poll, factory.getCarFeedbackPoll(carContext))
        assertEquals(MapboxScreen.ACTIVE_GUIDANCE, factory.getSourceName())
        verify(exactly = 1) { pollProvider.getActiveGuidanceFeedbackPoll(carContext) }
    }

    @Test
    fun `route preview factory uses the route preview poll`() {
        val factory = RoutePreviewFeedbackScreenFactory(mapboxCarContext)

        assertSame(poll, factory.getCarFeedbackPoll(carContext))
        assertEquals(MapboxScreen.ROUTE_PREVIEW, factory.getSourceName())
        verify(exactly = 1) { pollProvider.getRoutePreviewFeedbackPoll(carContext) }
    }

    @Test
    fun `place feedback factories use the place poll`() {
        val factories = listOf(
            SearchPlacesFeedbackScreenFactory(mapboxCarContext) to MapboxScreen.SEARCH,
            FavoritesFeedbackScreenFactory(mapboxCarContext) to MapboxScreen.FAVORITES,
            GeoDeeplinkPlacesFeedbackScreenFactory(mapboxCarContext) to MapboxScreen.GEO_DEEPLINK,
        )

        factories.forEach { (factory, sourceName) ->
            assertSame(poll, factory.getCarFeedbackPoll(carContext))
            assertEquals(sourceName, factory.getSourceName())
        }
        verify(exactly = 3) { pollProvider.getPlaceFeedbackPoll(carContext) }
    }

    private companion object {
        private const val TIMEOUT_MS = 2000L
    }
}
