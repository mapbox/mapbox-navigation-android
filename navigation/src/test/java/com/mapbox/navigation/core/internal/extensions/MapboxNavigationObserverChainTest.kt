package com.mapbox.navigation.core.internal.extensions

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.testutil.TestMapboxNavigationObserver
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapboxNavigationObserverChainTest {

    private lateinit var sut: MapboxNavigationObserverChain

    @Before
    fun setUp() {
        sut = MapboxNavigationObserverChain()
    }

    @Test
    fun `onAttached - should attach all child observers`() {
        val mapboxNavigation = mockk<MapboxNavigation>()
        val observers = arrayOf(
            TestMapboxNavigationObserver(),
            TestMapboxNavigationObserver(),
            TestMapboxNavigationObserver(),
        )
        sut.addAll(*observers)

        sut.onAttached(mapboxNavigation)

        observers.forEach {
            assertEquals(mapboxNavigation, it.attachedTo)
        }
    }

    @Test
    fun `onDetached - should detach all child observers`() {
        val mapboxNavigation = mockk<MapboxNavigation>()
        val observers = arrayOf(
            TestMapboxNavigationObserver(),
            TestMapboxNavigationObserver(),
            TestMapboxNavigationObserver(),
        )
        sut.addAll(*observers)
        sut.onAttached(mapboxNavigation)

        sut.onDetached(mapboxNavigation)

        observers.forEach {
            assertEquals(null, it.attachedTo)
        }
    }

    @Test
    fun `removeAndDetach - should remove and detach child observer`() {
        val mapboxNavigation = mockk<MapboxNavigation>()
        val firstObserver = spyk(TestMapboxNavigationObserver())
        val observers = arrayOf(
            firstObserver,
            TestMapboxNavigationObserver(),
            TestMapboxNavigationObserver(),
        )
        sut.addAll(*observers)
        sut.onAttached(mapboxNavigation)

        sut.removeAndDetach(firstObserver)
        sut.removeAndDetach(firstObserver) // testing idempotency

        assertEquals(null, firstObserver.attachedTo)
        verify(exactly = 1) { firstObserver.onDetached(any()) }
    }

    @Test
    fun `concurrent modification test`() {
        val mapboxNavigation = mockk<MapboxNavigation>()
        val selfRemovingObserver = object : TestMapboxNavigationObserver() {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                super.onAttached(mapboxNavigation)
                // doing some work here and immediately detaching
                sut.removeAndDetach(this)
            }
        }
        val observers = arrayOf(
            TestMapboxNavigationObserver(),
            selfRemovingObserver,
            TestMapboxNavigationObserver(),
        )
        sut.addAll(*observers)
        sut.onAttached(mapboxNavigation)

        assertEquals(null, selfRemovingObserver.attachedTo)
    }
}
