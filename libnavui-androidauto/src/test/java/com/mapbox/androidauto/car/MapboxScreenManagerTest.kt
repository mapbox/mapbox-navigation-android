package com.mapbox.androidauto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.androidauto.ActiveGuidanceState
import com.mapbox.androidauto.ArrivalState
import com.mapbox.androidauto.CarAppState
import com.mapbox.androidauto.FreeDriveState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.RoutePreviewState
import com.mapbox.androidauto.car.feedback.ui.CarGridFeedbackScreen
import com.mapbox.androidauto.car.navigation.ActiveGuidanceScreen
import com.mapbox.androidauto.car.permissions.NeedsLocationPermissionsScreen
import com.mapbox.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(
    MapboxExperimental::class,
    ExperimentalPreviewMapboxNavigationAPI::class,
    ExperimentalCoroutinesApi::class,
)
class MapboxScreenManagerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val screenManager = mockk<ScreenManager>(relaxed = true)
    private val mockCarContext = mockk<CarContext> {
        every { getCarService(ScreenManager::class.java) } returns screenManager
    }
    private val mainCarContext = mockk<MainCarContext> {
        every { carContext } returns mockCarContext
    }
    private val screenProvider = mockk<MapboxScreenProvider>(relaxed = true)

    private val sut = MapboxScreenManager(mainCarContext, screenProvider)

    @Before
    fun setup() {
        mockkStatic(PermissionsManager::class)
        mockkObject(MapboxCarApp)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `currentScreen is NeedsLocationPermissionsScreen permissions are not accepted`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
        every {
            screenProvider.needsLocationPermission()
        } returns mockk<NeedsLocationPermissionsScreen>()

        val screen = sut.currentScreen()

        assertThat(screen, instanceOf(NeedsLocationPermissionsScreen::class.java))
    }

    @Test
    fun `currentScreen is not NeedsLocationPermissionsScreen when permissions are accepted`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        every { MapboxCarApp.carAppState } returns MutableStateFlow(FreeDriveState)
        setupScreenProvider()

        val screen = sut.currentScreen()

        assertThat(screen, instanceOf(MainCarScreen::class.java))
    }

    @Test
    fun `onAttached changes screens when CarAppState changes`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            val appStateFlow = MutableStateFlow<CarAppState>(FreeDriveState)
            every { MapboxCarApp.carAppState } returns appStateFlow
            val pushedScreens = mutableListOf<Screen>()
            every { screenManager.push(capture(pushedScreens)) } just Runs
            setupScreenProvider()

            sut.onAttached(mockk())
            appStateFlow.value = mockk<RoutePreviewState>()
            appStateFlow.value = mockk<ActiveGuidanceState>()
            appStateFlow.value = mockk<ArrivalState>()
            appStateFlow.value = mockk<FreeDriveState>()
            sut.onDetached(mockk())

            assertEquals(5, pushedScreens.size)
            assertThat(pushedScreens[0], instanceOf(MainCarScreen::class.java))
            assertThat(pushedScreens[1], instanceOf(CarRoutePreviewScreen::class.java))
            assertThat(pushedScreens[2], instanceOf(ActiveGuidanceScreen::class.java))
            assertThat(pushedScreens[3], instanceOf(CarGridFeedbackScreen::class.java))
            assertThat(pushedScreens[4], instanceOf(MainCarScreen::class.java))
        }

    @Test
    fun `onAttached will not push screen if it is on top`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            val appStateFlow = MutableStateFlow<CarAppState>(FreeDriveState)
            every { MapboxCarApp.carAppState } returns appStateFlow
            val pushedScreens = mutableListOf<Screen>()
            every { screenManager.push(capture(pushedScreens)) } answers {
                every { screenManager.top } returns firstArg()
            }
            setupScreenProvider()

            sut.onAttached(mockk())
            appStateFlow.value = mockk<RoutePreviewState>()
            appStateFlow.value = mockk<RoutePreviewState>()
            appStateFlow.value = mockk<RoutePreviewState>()
            sut.onDetached(mockk())

            assertEquals(2, pushedScreens.size)
            assertThat(pushedScreens[0], instanceOf(MainCarScreen::class.java))
            assertThat(pushedScreens[1], instanceOf(CarRoutePreviewScreen::class.java))
        }

    @Test
    fun `onAttached will create 1 screen per state change`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            val appStateFlow = MutableStateFlow<CarAppState>(FreeDriveState)
            every { MapboxCarApp.carAppState } returns appStateFlow
            val pushedScreens = mutableListOf<Screen>()
            every { screenManager.push(capture(pushedScreens)) } answers {
                every { screenManager.top } returns firstArg()
            }
            setupScreenProvider()

            sut.onAttached(mockk())
            appStateFlow.value = mockk<FreeDriveState>()
            appStateFlow.value = mockk<FreeDriveState>()
            appStateFlow.value = mockk<RoutePreviewState>()
            appStateFlow.value = mockk<RoutePreviewState>()
            sut.onDetached(mockk())

            verify(exactly = 1) {
                screenProvider.freeDriveScreen()
                screenProvider.routePreviewScreen(any())
            }
        }

    @Test
    fun `onAttached screen changes will clear the backstack`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            val appStateFlow = MutableStateFlow<CarAppState>(FreeDriveState)
            every { MapboxCarApp.carAppState } returns appStateFlow
            val pushedScreens = mutableListOf<Screen>()
            every { screenManager.push(capture(pushedScreens)) } answers {
                every { screenManager.top } returns firstArg()
            }
            setupScreenProvider()

            sut.onAttached(mockk())

            verifyOrder {
                val oldScreen = screenManager.top
                screenManager.popToRoot()
                screenManager.push(any<MainCarScreen>())
                oldScreen.finish()
            }
        }

    @Test
    fun `replaceTop should pop and push a new screen`() {
        sut.replaceTop(mockk<MainCarScreen>())

        verifyOrder {
            val oldScreen = screenManager.top
            screenManager.popToRoot()
            screenManager.push(any<MainCarScreen>())
            oldScreen.finish()
        }
    }

    @Test
    fun `replaceTop should not change screen when the class has not changed`() {
        every { screenManager.top } returns mockk<MainCarScreen>()

        sut.replaceTop(mockk<MainCarScreen>())

        verify(exactly = 0) {
            screenManager.popToRoot()
            screenManager.push(any())
        }
    }

    private fun setupScreenProvider() {
        every {
            screenProvider.needsLocationPermission()
        } returns mockk<NeedsLocationPermissionsScreen>()
        every { screenProvider.freeDriveScreen() } returns mockk<MainCarScreen>()
        every { screenProvider.routePreviewScreen(any()) } returns mockk<CarRoutePreviewScreen>()
        every { screenProvider.activeGuidanceScreen() } returns mockk<ActiveGuidanceScreen>()
        every { screenProvider.arrivalScreen() } returns mockk<CarGridFeedbackScreen>()
    }
}
