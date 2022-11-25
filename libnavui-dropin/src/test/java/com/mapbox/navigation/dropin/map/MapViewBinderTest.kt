package com.mapbox.navigation.dropin.map

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.camera.CameraComponent
import com.mapbox.navigation.dropin.camera.CameraLayoutObserver
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.map.geocoding.GeocodingComponent
import com.mapbox.navigation.dropin.map.logo.LogoAttributionComponent
import com.mapbox.navigation.dropin.map.longpress.FreeDriveLongPressMapComponent
import com.mapbox.navigation.dropin.map.longpress.RoutePreviewLongPressMapComponent
import com.mapbox.navigation.dropin.map.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.map.scalebar.ScalebarComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.ui.maps.internal.ui.BuildingHighlightComponent
import com.mapbox.navigation.ui.maps.internal.ui.LocationComponent
import com.mapbox.navigation.ui.maps.internal.ui.LocationPuckComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapViewBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var store: TestStore
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: MapViewBinder
    private lateinit var loadedMapStyleFlow: MutableStateFlow<Style?>
    private lateinit var mapView: MapView
    private lateinit var mapViewOwner: MapViewOwner

    @Suppress("PrivatePropertyName")
    private var MAP_STYLE = mockk<Style> {
        every { styleURI } returns Style.DARK
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        store = TestStore()
        mapViewOwner = mockk(relaxed = true)
        navContext = spyk(
            NavigationViewContext(
                context = ctx,
                lifecycleOwner = TestLifecycleOwner(),
                viewModel = NavigationViewModel(),
                storeProvider = { store }
            )
        )
        every { navContext.mapViewOwner } returns mapViewOwner
        loadedMapStyleFlow = MutableStateFlow(null)
        every { navContext.mapStyleLoader } returns mockk {
            every { loadedMapStyle } returns loadedMapStyleFlow
        }
        mapboxNavigation = mockk(relaxed = true)
        mapView = mockk(relaxed = true) {
            every { getMapboxMap() } returns mockk(relaxed = true) {
                every { getStyle() } returns null
            }
            every { compass } returns mockk(relaxed = true)
            every { scalebar } returns mockk(relaxed = true)
            every { location } returns mockk(relaxed = true)
            every { attribution } returns mockk(relaxed = true)
            every { logo } returns mockk(relaxed = true)
            every { gestures } returns mockk(relaxed = true)
            every { annotations } returns mockk(relaxed = true) {
                every { createPointAnnotationManager() } returns mockk(relaxed = true)
            }
            every { camera } returns mockk(relaxUnitFun = true) {
                every { addCameraAnimationsLifecycleListener(any()) } just Runs
            }
            every { context } returns ctx
            every { resources } returns ctx.resources
            every { width } returns 100
            every { height } returns 100
            every { parent } returns null
        }

        sut = object : MapViewBinder() {
            override fun getMapView(context: Context): MapView = mapView
        }
        sut.context = navContext
        sut.navigationViewBinding = MapboxNavigationViewLayoutBinding.inflate(
            LayoutInflater.from(ctx),
            FrameLayout(ctx)
        )
    }

    @Test
    fun `bind should return and bind CameraLayoutObserver`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is CameraLayoutObserver })
    }

    @Test
    fun `bind should return and bind LocationComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is LocationComponent })
    }

    @Test
    fun `bind should reload LocationPuckComponent on locationPuck style change`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is LocationPuckComponent }
        navContext.applyStyleCustomization {
            locationPuckOptions = LocationPuckOptions
                .Builder(ctx)
                .freeDrivePuck(
                    LocationPuck2D(
                        bearingImage = ctx.getDrawable(android.R.drawable.arrow_down_float)
                    )
                )
                .build()
        }
        val secondComponent = components.findComponent { it is LocationPuckComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind should reload LocationPuckComponent on navigation state change`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is LocationPuckComponent }
        store.updateState { it.copy(navigation = NavigationState.RoutePreview) }
        val secondComponent = components.findComponent { it is LocationPuckComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind should return and bind LogoAttributionComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is LogoAttributionComponent })
    }

    @Test
    fun `bind should reload RouteLineComponent on mapStyle change`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is RouteLineComponent }
        loadedMapStyleFlow.value = MAP_STYLE
        val secondComponent = components.findComponent { it is RouteLineComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind should reload RouteLineComponent on routeLineOptions change`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is RouteLineComponent }
        navContext.applyOptionsCustomization {
            routeLineOptions = MapboxRouteLineOptions.Builder(ctx)
                .withVanishingRouteLineEnabled(true)
                .build()
        }
        val secondComponent = components.findComponent { it is RouteLineComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind should return and bind CameraComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is CameraComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should reload MapMarkersComponent on destinationMarkerAnnotationOptions style change`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is MapMarkersComponent }
        navContext.applyStyleCustomization {
            destinationMarkerAnnotationOptions = PointAnnotationOptions()
        }
        val secondComponent = components.findComponent { it is MapMarkersComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind should attach correct longPressMapComponent for each NavigationState`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        arrayOf(
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview,
            NavigationState.RoutePreview,
            NavigationState.ActiveNavigation,
            NavigationState.Arrival,
        ).forEach { navState ->
            store.updateState { it.copy(navigation = navState) }
            when (navState) {
                NavigationState.FreeDrive,
                NavigationState.DestinationPreview -> {
                    val c = components.findComponent { it is FreeDriveLongPressMapComponent }
                    assertNotNull("Should attach for $navState", c)
                }
                NavigationState.RoutePreview -> {
                    val c = components.findComponent { it is RoutePreviewLongPressMapComponent }
                    assertNotNull("Should attach for $navState", c)
                }
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> {
                    val c = components.findComponent {
                        it is RoutePreviewLongPressMapComponent ||
                            it is FreeDriveLongPressMapComponent
                    }
                    assertNull("Should not attach for $navState", c)
                }
            }
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should attach GeocodingComponent only for FreeDrive DestinationPreview and RoutePreview NavigationState`() {
        val expectation = mapOf(
            NavigationState.FreeDrive to true,
            NavigationState.DestinationPreview to true,
            NavigationState.RoutePreview to true,
            NavigationState.ActiveNavigation to false,
            NavigationState.Arrival to false,
        )
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        expectation.forEach { (navigationState, shouldBind) ->
            store.updateState { it.copy(navigation = navigationState) }
            val c = components.findComponent { it is GeocodingComponent }
            if (shouldBind) {
                assertNotNull("Should attach GeocodingComponent for $navigationState", c)
            } else {
                assertNull("Should not attach GeocodingComponent for $navigationState", c)
            }
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should reload RouteArrowComponent on arrowOptions change`() {
        store.updateState { it.copy(navigation = NavigationState.ActiveNavigation) }
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is RouteArrowComponent }
        navContext.applyOptionsCustomization {
            routeArrowOptions = RouteArrowOptions.Builder(ctx)
                .withArrowColor(Color.RED)
                .build()
        }
        val secondComponent = components.findComponent { it is RouteArrowComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should attach RouteArrowComponent only for ActiveNavigation NavigationState`() {
        val expectation = mapOf(
            NavigationState.FreeDrive to false,
            NavigationState.DestinationPreview to false,
            NavigationState.RoutePreview to false,
            NavigationState.ActiveNavigation to true,
            NavigationState.Arrival to false,
        )
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        expectation.forEach { (navigationState, shouldBind) ->
            store.updateState { it.copy(navigation = navigationState) }
            val c = components.findComponent { it is RouteArrowComponent }
            if (shouldBind) {
                assertNotNull("Should attach RouteArrowComponent for $navigationState", c)
            } else {
                assertNull("Should not attach RouteArrowComponent for $navigationState", c)
            }
        }
    }

    @Test
    fun `bind should reload RouteArrowComponent on mapStyle change`() {
        store.updateState { it.copy(navigation = NavigationState.ActiveNavigation) }
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is RouteArrowComponent }
        loadedMapStyleFlow.value = MAP_STYLE
        val secondComponent = components.findComponent { it is RouteArrowComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind should return and bind ScalebarComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is ScalebarComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should attach BuildingHighlightComponent when NavigationViewOptions_enableBuildingHighlightOnArrival is TRUE`() {
        navContext.applyOptionsCustomization {
            enableBuildingHighlightOnArrival = true
        }

        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is BuildingHighlightComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should NOT attach BuildingHighlightComponent when NavigationViewOptions_enableBuildingHighlightOnArrival is FALSE`() {
        navContext.applyOptionsCustomization {
            enableBuildingHighlightOnArrival = false
        }

        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNull(components.findComponent { it is BuildingHighlightComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should reload BuildingHighlightComponent on NavigationViewOptions_buildingHighlightOptions change`() {
        navContext.applyOptionsCustomization {
            enableBuildingHighlightOnArrival = true
            buildingHighlightOptions = MapboxBuildingHighlightOptions.Builder().build()
        }
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        val firstComponent = components.findComponent { it is BuildingHighlightComponent }
        navContext.applyOptionsCustomization {
            buildingHighlightOptions = MapboxBuildingHighlightOptions.Builder()
                .fillExtrusionOpacity(0.9)
                .build()
        }
        val secondComponent = components.findComponent { it is BuildingHighlightComponent }

        assertNotNull(firstComponent)
        assertNotNull(secondComponent)
        assertNotEquals(secondComponent, firstComponent)
    }

    @Test
    fun `bind adds mapView to layout`() {
        val viewGroup = spyk(FrameLayout(ctx))

        sut.bind(viewGroup)

        verify {
            viewGroup.removeAllViews()
            viewGroup.addView(
                mapView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    @Test
    fun `bind updates mapView`() {
        sut.bind(FrameLayout(ctx))
        verify { mapViewOwner.updateMapView(mapView) }
    }

    @Test
    fun `shouldLoadMapStyle should be false`() {
        assertFalse(sut.shouldLoadMapStyle)
    }
}
