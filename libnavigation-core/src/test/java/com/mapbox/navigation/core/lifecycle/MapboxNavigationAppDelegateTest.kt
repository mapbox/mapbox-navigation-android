@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import androidx.lifecycle.Lifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalPreviewMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
class MapboxNavigationAppDelegateTest {

    private val mapboxNavigation: MapboxNavigation = mockk()
    private val navigationOptions: NavigationOptions = mockk {
        every { accessToken } returns "test_access_token"
    }

    private val mapboxNavigationApp = MapboxNavigationAppDelegate()

    @Before
    fun setup() {
        mockkStatic(MapboxNavigationProvider::class)
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockk(relaxUnitFun = true)
        every { MapboxNavigationProvider.create(navigationOptions) } returns mapboxNavigation
        every { MapboxNavigationProvider.retrieve() } returns mapboxNavigation
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `verify onAttached and onDetached when multiple lifecycles have started`() {
        mapboxNavigationApp.setup(navigationOptions)

        val testLifecycleOwnerA = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        val testLifecycleOwnerB = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwnerA)
        mapboxNavigationApp.attach(testLifecycleOwnerB)

        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)
        mapboxNavigationApp.unregisterObserver(observer)

        verifyOrder {
            observer.onAttached(any())
            observer.onDetached(any())
        }
    }

    @Test
    fun `verify onAttached is called when LifecycleOwner is started`() {
        mapboxNavigationApp.setup(navigationOptions)

        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)

        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        verify(exactly = 1) { firstObserver.onAttached(any()) }
        verify(exactly = 1) { secondObserver.onAttached(any()) }
        verify(exactly = 0) { firstObserver.onDetached(any()) }
        verify(exactly = 0) { secondObserver.onDetached(any()) }
    }

    @Test
    fun `verify setup can be called after LifecycleOwner is started`() {
        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)

        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup(navigationOptions)

        verify(exactly = 1) { firstObserver.onAttached(any()) }
        verify(exactly = 1) { secondObserver.onAttached(any()) }
        verify(exactly = 0) { firstObserver.onDetached(any()) }
        verify(exactly = 0) { secondObserver.onDetached(any()) }
    }

    @Test
    fun `verify detaching all LifecycleOwners detaches all observers`() {
        mapboxNavigationApp.setup(navigationOptions)

        val testLifecycleOwnerA = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        val testLifecycleOwnerB = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwnerA)
        mapboxNavigationApp.attach(testLifecycleOwnerB)

        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        mapboxNavigationApp.detach(testLifecycleOwnerA)
        mapboxNavigationApp.detach(testLifecycleOwnerB)

        verifyOrder {
            firstObserver.onAttached(any())
            secondObserver.onAttached(any())
            firstObserver.onDetached(any())
            secondObserver.onDetached(any())
        }
    }

    @Test
    fun `verify disable will call observers onDetached`() {
        mapboxNavigationApp.setup(navigationOptions)

        val testLifecycleOwnerA = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        val testLifecycleOwnerB = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwnerA)
        mapboxNavigationApp.attach(testLifecycleOwnerB)

        testLifecycleOwnerA.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwnerB.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        mapboxNavigationApp.disable()

        verifyOrder {
            firstObserver.onAttached(any())
            secondObserver.onAttached(any())
            firstObserver.onDetached(any())
            secondObserver.onDetached(any())
        }
    }

    @Test
    fun `verify disable will prevent mapboxNavigation from restarting`() {
        mapboxNavigationApp.setup(navigationOptions)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)

        mapboxNavigationApp.disable()
        mapboxNavigationApp.detach(testLifecycleOwner)
        mapboxNavigationApp.attach(testLifecycleOwner)

        verify(exactly = 1) { observer.onAttached(any()) }
        verify(exactly = 1) { observer.onAttached(any()) }
    }

    @Test
    fun `verify current is null when all lifecycle owners are destroyed`() {
        mapboxNavigationApp.setup(navigationOptions)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)

        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        assertNull(mapboxNavigationApp.current())
    }

    @Test
    fun `verify current is set after LifecycleOwner is created`() {
        mapboxNavigationApp.setup(navigationOptions)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)

        assertNull(mapboxNavigationApp.current())
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        assertNotNull(mapboxNavigationApp.current())
    }
}
