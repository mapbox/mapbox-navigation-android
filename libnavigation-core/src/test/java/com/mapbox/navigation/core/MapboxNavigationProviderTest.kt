package com.mapbox.navigation.core

import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowReachabilityFactory::class])
internal class MapboxNavigationProviderTest : MapboxNavigationBaseTest() {

    private val observer = mockk<MapboxNavigationObserver>(relaxed = true)

    @Before
    override fun setUp() {
        super.setUp()
        MapboxNavigationProvider.destroy()
    }

    @After
    override fun tearDown() {
        MapboxNavigationProvider.destroy()
        super.tearDown()
    }

    @Test
    fun registerObserver_noNavigation() {
        MapboxNavigationProvider.registerObserver(observer)

        verify(exactly = 0) {
            observer.onDetached(any())
            observer.onAttached(any())
        }
    }

    @Test
    fun registerObserver_hasNavigation() {
        val navigation = MapboxNavigationProvider.create(navigationOptions)
        MapboxNavigationProvider.registerObserver(observer)

        verify(exactly = 1) {
            observer.onAttached(navigation)
        }
        verify(exactly = 0) {
            observer.onDetached(any())
        }
    }

    @Test
    fun unregisterObserver_noNavigation() {
        MapboxNavigationProvider.unregisterObserver(observer)

        verify(exactly = 0) {
            observer.onDetached(any())
            observer.onAttached(any())
        }
    }

    @Test
    fun unregisterObserver_hasNavigation() {
        val navigation = MapboxNavigationProvider.create(navigationOptions)
        MapboxNavigationProvider.unregisterObserver(observer)

        verify(exactly = 1) {
            observer.onDetached(navigation)
        }
        verify(exactly = 0) {
            observer.onAttached(any())
        }
    }

    @Test
    fun navigationChanges() {
        MapboxNavigationProvider.registerObserver(observer)

        val navigation1 = MapboxNavigationProvider.create(navigationOptions)

        verify(exactly = 1) { observer.onAttached(navigation1) }

        MapboxNavigationProvider.destroy()

        verify(exactly = 1) { observer.onDetached(navigation1) }

        val navigation2 = MapboxNavigationProvider.create(navigationOptions)

        verify(exactly = 1) { observer.onAttached(navigation2) }
    }
}
