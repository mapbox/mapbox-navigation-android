package com.mapbox.navigation.dropin

import androidx.activity.ComponentActivity
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.actionbutton.ActionButtonsCoordinator
import com.mapbox.navigation.dropin.analytics.AnalyticsComponent
import com.mapbox.navigation.dropin.backpress.BackPressedComponent
import com.mapbox.navigation.dropin.infopanel.InfoPanelCoordinator
import com.mapbox.navigation.dropin.maneuver.ManeuverCoordinator
import com.mapbox.navigation.dropin.map.MapLayoutCoordinator
import com.mapbox.navigation.dropin.map.MapViewObserver
import com.mapbox.navigation.dropin.map.scalebar.ScalebarPlaceholderCoordinator
import com.mapbox.navigation.dropin.navigationview.NavigationViewListener
import com.mapbox.navigation.dropin.permission.LocationPermissionComponent
import com.mapbox.navigation.dropin.roadname.RoadNameCoordinator
import com.mapbox.navigation.dropin.speedlimit.SpeedLimitCoordinator
import com.mapbox.navigation.dropin.tripsession.TripSessionComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.app.internal.SharedApp
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class NavigationViewTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var activity: ComponentActivity
    private lateinit var sut: NavigationView

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var registeredObservers: MutableList<Any>
    private lateinit var windowInsetsListener: CapturingSlot<OnApplyWindowInsetsListener>

    @Before
    fun setUp() {
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns mockk()

        val controller = buildActivity(ComponentActivity::class.java).setup()
        activity = controller.get()
        windowInsetsListener = slot()
        mockkStatic(ViewCompat::setOnApplyWindowInsetsListener)
        every {
            ViewCompat.setOnApplyWindowInsetsListener(any(), capture(windowInsetsListener))
        } returns Unit

        mockkObject(SharedApp)
        every { SharedApp.setup() } returns Unit

        mapboxNavigation = mockk(relaxed = true)

        registeredObservers = mutableListOf()
        mockkStatic(MapboxNavigationApp::class)
        every { MapboxNavigationApp.isSetup() } returns true
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        every { MapboxNavigationApp.attach(any()) } returns MapboxNavigationApp
        every { MapboxNavigationApp.registerObserver(any()) } answers {
            registeredObservers.add(args.first()!!)
            MapboxNavigationApp
        }

        // NavigationView
        sut = NavigationView(activity, null, "pk.access_token")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init`() {
        assertNotNull(sut)
    }

    @Test
    fun `init should register components`() {
        (sut.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.CREATED

        verifyRegisteredObserver(AnalyticsComponent::class)
        verifyRegisteredObserver(LocationPermissionComponent::class)
        verifyRegisteredObserver(TripSessionComponent::class)
        verifyRegisteredObserver(MapLayoutCoordinator::class)
        verifyRegisteredObserver(BackPressedComponent::class)
        verifyRegisteredObserver(ScalebarPlaceholderCoordinator::class)
        verifyRegisteredObserver(ManeuverCoordinator::class)
        verifyRegisteredObserver(InfoPanelCoordinator::class)
        verifyRegisteredObserver(ActionButtonsCoordinator::class)
        verifyRegisteredObserver(SpeedLimitCoordinator::class)
        verifyRegisteredObserver(RoadNameCoordinator::class)
        verifyRegisteredObserver(LeftFrameCoordinator::class)
        verifyRegisteredObserver(RightFrameCoordinator::class)
    }

    @Test
    fun `init should setup SharedApp`() {
        verify { SharedApp.setup() }
    }

    @Test
    fun `init should setup MapboxNavigationApp`() {
        every { MapboxNavigationApp.isSetup() } returns false
        every {
            MapboxNavigationApp.setup(ofType(NavigationOptions::class))
        } returns MapboxNavigationApp

        NavigationView(activity, null, "pk.access_token")

        verify { MapboxNavigationApp.setup(ofType(NavigationOptions::class)) }
    }

    @Test
    fun `init should attach itself to MapboxNavigationApp`() {
        verify { MapboxNavigationApp.attach(sut) }
    }

    @Test
    fun `init should set OnApplyWindowInsetsListener to capture system bars insets`() {
        val systemBarsInsets = Insets.of(1, 2, 3, 4)
        val windowInsets = mockk<WindowInsetsCompat> {
            every { getInsets(WindowInsetsCompat.Type.systemBars()) } returns systemBarsInsets
        }

        windowInsetsListener.captured.onApplyWindowInsets(sut, windowInsets)

        assertEquals(systemBarsInsets, sut.navigationContext.systemBarsInsets.value)
    }

    @Test
    fun `customizeViewBinders call NavigationViewContext`() {
        val binder = EmptyBinder()
        sut.customizeViewBinders {
            roadNameBinder = binder
        }

        assertEquals(binder, sut.navigationContext.uiBinders.roadName.value)
    }

    @Test
    fun `customizeViewStyles call NavigationViewContext`() {
        sut.customizeViewStyles {
            arrivalTextAppearance = R.style.TextAppearance_AppCompat
        }

        assertEquals(
            R.style.TextAppearance_AppCompat,
            sut.navigationContext.styles.arrivalTextAppearance.value
        )
    }

    @Test
    fun `customizeViewOptions call NavigationViewContext`() {
        sut.customizeViewOptions {
            infoPanelForcedState = BottomSheetBehavior.STATE_HIDDEN
        }

        assertEquals(
            BottomSheetBehavior.STATE_HIDDEN,
            sut.navigationContext.options.infoPanelForcedState.value
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `addListener should add NavigationViewListener to NavigationViewListenerRegistry`() {
        val listener = object : NavigationViewListener() {}

        sut.addListener(listener)

        assertNotNull(
            sut.navigationContext.listenerRegistry
                .getRegisteredListeners()
                .firstOrNull { it == listener }
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `removeListener should remove NavigationViewListener from NavigationViewListenerRegistry`() {
        val listener = object : NavigationViewListener() {}
        sut.addListener(listener)

        sut.removeListener(listener)

        assertNull(
            sut.navigationContext.listenerRegistry
                .getRegisteredListeners()
                .firstOrNull { it == listener }
        )
    }

    @Test
    fun `registerMapObserver should register MapViewObserver with MapViewOwner`() {
        val observer = object : MapViewObserver() {}

        sut.registerMapObserver(observer)

        assertNotNull(
            sut.navigationContext.mapViewOwner
                .getRegisteredObservers()
                .firstOrNull { it == observer }
        )
    }

    @Test
    fun `unregisterMapObserver should unregister MapViewObserver from MapViewOwner`() {
        val observer = object : MapViewObserver() {}
        sut.registerMapObserver(observer)

        sut.unregisterMapObserver(observer)

        assertNull(
            sut.navigationContext.mapViewOwner
                .getRegisteredObservers()
                .firstOrNull { it == observer }
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `setRouteOptionsInterceptor should use RouteOptionsInterceptor to update RouteOptions`() {
        sut.setRouteOptionsInterceptor {
            it.baseUrl("https://example.com")
        }

        val options = sut.navigationContext.routeOptionsProvider
            .getOptions(
                mapboxNavigation,
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(10.0, 10.0),
            )

        assertEquals("https://example.com", options.baseUrl())
    }

    @Test
    fun `setRouteOptionsInterceptor with NULL should remove the RouteOptionsInterceptor`() {
        sut.setRouteOptionsInterceptor {
            it.baseUrl("https://example.com")
        }
        sut.setRouteOptionsInterceptor(null)

        val options = sut.navigationContext.routeOptionsProvider
            .getOptions(
                mapboxNavigation,
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(10.0, 10.0),
            )

        assertNotEquals("https://example.com", options.baseUrl())
    }

    private inline fun <reified T : Any> verifyRegisteredObserver(t: KClass<T>) {
        assertNotNull(
            "Expected to register MapboxNavigationObserver of type ${t.simpleName}",
            registeredObservers.firstOrNull { t.isInstance(it) }
        )
    }
}
