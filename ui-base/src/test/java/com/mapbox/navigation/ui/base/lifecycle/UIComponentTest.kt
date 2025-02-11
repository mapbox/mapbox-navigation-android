package com.mapbox.navigation.ui.base.lifecycle

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UIComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `should collect state when attached`() = runBlockingTest {
        val component = TestUIComponent(10)

        component.onAttached(mockk())

        assertEquals(10, component.captured[0])
    }

    @Test
    fun `should not collect state before attached`() = runBlockingTest {
        val component = TestUIComponent(10)

        assertTrue(component.captured.isEmpty())
    }

    @Test
    fun `should not collect events after detached`() = runBlockingTest {
        val component = TestUIComponent(1)

        val mapboxNavigation = mockk<MapboxNavigation>()
        component.onAttached(mapboxNavigation)
        component.stateFlow.emit(2)
        component.onDetached(mapboxNavigation)
        component.stateFlow.emit(3)

        assertEquals(listOf(1, 2), component.captured)
    }

    @Test
    fun `two components can be attached and detached independently`() = runBlockingTest {
        val firstComponent = TestUIComponent(1)
        val secondComponent = TestUIComponent(2)

        val mapboxNavigation = mockk<MapboxNavigation>()
        firstComponent.onAttached(mapboxNavigation)
        secondComponent.onAttached(mapboxNavigation)
        firstComponent.stateFlow.emit(3)
        firstComponent.onDetached(mapboxNavigation)
        secondComponent.stateFlow.emit(4)

        assertEquals(listOf(1, 3), firstComponent.captured)
        assertEquals(listOf(2, 4), secondComponent.captured)
    }

    @Test
    fun `should be able to re-attach`() = runBlockingTest {
        val component = TestUIComponent(1)

        val mapboxNavigation = mockk<MapboxNavigation>()
        component.onAttached(mapboxNavigation)
        component.stateFlow.emit(2)
        component.onDetached(mapboxNavigation)
        component.onAttached(mapboxNavigation)
        component.stateFlow.emit(3)
        component.onDetached(mapboxNavigation)

        assertEquals(listOf(1, 2, 2, 3), component.captured)
    }
}

class TestUIComponent(initialValue: Int) : UIComponent() {
    val captured = mutableListOf<Int>()
    val stateFlow = MutableStateFlow(initialValue)
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            stateFlow.collect {
                captured.add(it)
            }
        }
    }
}
