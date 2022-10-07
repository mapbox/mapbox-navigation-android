package com.mapbox.navigation.ui.app.internal

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.testing.TestReducer
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@ExperimentalPreviewMapboxNavigationAPI
internal class StoreTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: TestStore

    @Before
    fun setUp() {
        sut = TestStore()
    }

    @Test
    fun `dispatch should call each reducer and update state`() {
        sut.register(
            Reducer { state, _ ->
                state.copy(navigation = NavigationState.ActiveNavigation)
            }
        )
        sut.register(
            Reducer { state, _ ->
                state.copy(
                    previewRoutes = mockk<RoutePreviewState.Fetching> {
                        every { requestId } returns 0
                    }
                )
            }
        )

        sut.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))

        val routesState = sut.state.value.previewRoutes as RoutePreviewState.Fetching
        assertEquals(0, routesState.requestId)
        assertEquals(NavigationState.ActiveNavigation, sut.state.value.navigation)
    }

    @Test
    fun `dispatch should not accept new actions before all reducers finish`() {
        sut.register(
            Reducer { state, action ->
                if (action is NavigationStateAction.Update) {
                    state.copy(navigation = action.state)
                } else {
                    state
                }
            }
        )
        sut.register(
            Reducer { state, _ ->
                sut.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
                state
            }
        )

        sut.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))

        assertEquals(NavigationState.ActiveNavigation, sut.state.value.navigation)
    }

    @Test
    fun `select should return flowable that emit only when state changes`() =
        coroutineRule.runBlockingTest {
            val collectedStates = mutableListOf<NavigationState>()
            launch {
                sut.select { it.navigation }.take(2).toList(collectedStates)
            }

            sut.setState(State())
            sut.setState(State(navigation = NavigationState.ActiveNavigation))

            advanceUntilIdle()
            val expected = listOf(NavigationState.FreeDrive, NavigationState.ActiveNavigation)
            assertEquals(expected, collectedStates)
        }

    @Test
    fun `reset should restore initial state`() {
        sut.setState(State(destination = Destination(Point.fromLngLat(1.0, 2.0))))
        assertNotEquals(State(), sut.state.value)

        sut.reset()
        assertEquals(State(), sut.state.value)
    }

    @Test
    fun `should allow action intercept with Middleware`() {
        val middleware = object : Middleware {
            override fun onDispatch(state: State, action: Action): Boolean {
                return (action is DestinationAction.SetDestination)
            }
        }
        val reducer = TestReducer()
        sut.registerMiddleware(middleware)
        sut.register(reducer)

        val action1 = DestinationAction.SetDestination(
            Destination(Point.fromLngLat(1.0, 2.0))
        )
        val action2 = CameraAction.SetCameraMode(TargetCameraMode.Overview)
        sut.dispatch(action1)
        sut.dispatch(action2)

        assertFalse(action1 in reducer.actions)
        assertTrue(action2 in reducer.actions)
    }

    @Test
    fun `should allow action dispatch from state observer`() = coroutineRule.runBlockingTest {
        val actionsCapture = TestReducer()
        sut.register(actionsCapture)
        sut.register(
            Reducer { state, action ->
                if (action is NavigationStateAction.Update) {
                    state.copy(navigation = action.state)
                } else {
                    state
                }
            }
        )
        launch {
            sut.select { it.navigation }
                .filter { it == NavigationState.RoutePreview }
                .take(1)
                .collect {
                    sut.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
                }
        }
        sut.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))

        assertEquals(
            listOf(
                NavigationStateAction.Update(NavigationState.RoutePreview),
                NavigationStateAction.Update(NavigationState.ActiveNavigation)
            ),
            actionsCapture.actions
        )
        assertEquals(NavigationState.ActiveNavigation, sut.state.value.navigation)
    }
}
