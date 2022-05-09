package com.mapbox.navigation.dropin.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@ExperimentalPreviewMapboxNavigationAPI
internal class StoreTest {

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
                state.copy(routes = RoutesState.Fetching(0))
            }
        )

        sut.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))

        assertEquals(RoutesState.Fetching(0), sut.state.value.routes)
        assertEquals(NavigationState.ActiveNavigation, sut.state.value.navigation)
    }

    @Test
    fun `dispatch should not accept new actions before all reducers finish`() {
        sut.register(
            Reducer { state, action ->
                if (action is NavigationStateAction.Update) state.copy(navigation = action.state)
                else state
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
}
