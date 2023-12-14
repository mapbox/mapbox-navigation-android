package com.mapbox.navigation.ui.maps.internal.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.bindgen.Expected
import com.mapbox.maps.Style
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.utils.internal.isVisible
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class RoadNameComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var roadNameView: MapboxRoadNameView
    private lateinit var contract: TestContract
    private lateinit var routeShieldApi: MapboxRouteShieldApi
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: RoadNameComponent

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns mockk()

        val context: Context = ApplicationProvider.getApplicationContext()
        contract = TestContract()
        roadNameView = spyk(MapboxRoadNameView(context))
        routeShieldApi = mockk()
        val navOptions = NavigationOptions.Builder(context).accessToken("access_token").build()
        mapboxNavigation = mockk {
            every { navigationOptions } returns navOptions
        }

        sut = RoadNameComponent(roadNameView, { contract }, routeShieldApi)
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(LocationEngineProvider::class)
    }

    @Test
    fun `onAttached should hide roadNameView when road name data is NOT available`() =
        coroutineRule.runBlockingTest {
            givenContractData(mapStyle = FIXTURES.mapStyle, roadData = FIXTURES.roadWithoutName)

            sut.onAttached(mapboxNavigation)

            assertFalse(roadNameView.isVisible)
        }

    @Test
    fun `onAttached should hide roadNameView if map style is NOT available`() =
        coroutineRule.runBlockingTest {
            givenContractData(mapStyle = null, roadData = FIXTURES.roadWithName)

            sut.onAttached(mapboxNavigation)

            assertFalse(roadNameView.isVisible)
        }

    @Test
    fun `onAttached should show roadNameView when road name data is available`() =
        coroutineRule.runBlockingTest {
            givenContractData(mapStyle = FIXTURES.mapStyle, roadData = FIXTURES.roadWithName)
            givenRouteShieldsResponse(FIXTURES.roadWithName, FIXTURES.shieldsResponse)

            sut.onAttached(mapboxNavigation)

            assertTrue(roadNameView.isVisible)
            verify { roadNameView.renderRoadName(FIXTURES.roadWithName) }
        }

    @Test
    fun `onAttached should use MapboxRouteShieldApi to render road shields`() =
        coroutineRule.runBlockingTest {
            givenContractData(mapStyle = FIXTURES.mapStyle, roadData = FIXTURES.roadWithName)
            givenRouteShieldsResponse(FIXTURES.roadWithName, FIXTURES.shieldsResponse)

            sut.onAttached(mapboxNavigation)

            verify { roadNameView.renderRoadNameWith(FIXTURES.shieldsResponse) }
        }

    private fun givenContractData(mapStyle: Style?, roadData: Road?) {
        contract.mapStyle.value = mapStyle
        contract.roadInfo.value = roadData
    }

    private fun givenRouteShieldsResponse(
        road: Road,
        shields: List<Expected<RouteShieldError, RouteShieldResult>>
    ) {
        val shieldsCallbackSlot = slot<RouteShieldCallback>()
        every {
            routeShieldApi.getRouteShields(road, any(), any(), any(), capture(shieldsCallbackSlot))
        } answers {
            shieldsCallbackSlot.captured.onRoadShields(shields)
        }
    }

    @Suppress("PrivatePropertyName")
    private val FIXTURES = object {
        val roadWithName = mockk<Road> {
            every { components } returns listOf(
                mockk {
                    every { text } returns "road text"
                    every { shield } returns mockk()
                    every { imageBaseUrl } returns null
                }
            )
        }

        val roadWithoutName = mockk<Road> {
            every { components } returns emptyList()
        }

        val mapStyle = mockk<Style> {
            every { styleURI } returns NavigationStyles.NAVIGATION_DAY_STYLE
        }

        val shieldsResponse: List<Expected<RouteShieldError, RouteShieldResult>> = listOf()
    }

    private class TestContract : RoadNameComponentContract {
        override val roadInfo: MutableStateFlow<Road?> = MutableStateFlow(null)
        override val mapStyle: MutableStateFlow<Style?> = MutableStateFlow(null)
    }
}
