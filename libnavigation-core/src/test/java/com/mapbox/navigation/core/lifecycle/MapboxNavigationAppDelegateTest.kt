@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import androidx.core.app.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
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
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `verify multiple setup calls are ignored`() {
        mapboxNavigationApp.setup(navigationOptions)
        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        mapboxNavigationApp.setup(navigationOptions)
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup(navigationOptions)

        verify(exactly = 1) { firstObserver.onAttached(any()) }
        verify(exactly = 1) { secondObserver.onAttached(any()) }
        verify(exactly = 0) { firstObserver.onDetached(any()) }
        verify(exactly = 0) { secondObserver.onDetached(any()) }
    }

    @Test
    fun `verify setup will not recreate mapboxNavigation when undergoing orientation change`() {
        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        val (portraitActivity, portraitLifecycle) = mockActivityLifecycle()
        portraitLifecycle.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup(navigationOptions)
        mapboxNavigationApp.attach(portraitActivity)
        every { portraitActivity.isChangingConfigurations } returns true
        portraitLifecycle.currentState = Lifecycle.State.DESTROYED

        val (landscapeActivity, landscapeLifecycle) = mockActivityLifecycle()
        landscapeLifecycle.currentState = Lifecycle.State.RESUMED
        every { landscapeActivity.isChangingConfigurations } returns false
        landscapeLifecycle.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup(navigationOptions)
        mapboxNavigationApp.attach(landscapeActivity)

        verify(exactly = 1) { firstObserver.onAttached(any()) }
        verify(exactly = 1) { secondObserver.onAttached(any()) }
        verify(exactly = 0) { firstObserver.onDetached(any()) }
        verify(exactly = 0) { secondObserver.onDetached(any()) }
    }

    @Test
    fun `verify lifecycleOwner resumed will have correct callbacks`() {
        val (activity, lifecycle) = mockActivityLifecycle()
        mapboxNavigationApp.attach(activity)

        val observer = mockk<DefaultLifecycleObserver>(relaxUnitFun = true)
        mapboxNavigationApp.lifecycleOwner.lifecycle.addObserver(observer)
        lifecycle.currentState = Lifecycle.State.RESUMED

        verifyOrder {
            observer.onCreate(any())
            observer.onStart(any())
            observer.onResume(any())
        }
    }

    // This test demonstrates why keeping track of the previous level matters.
    // Specifically the CarAppLifecycleOwner.foregroundedChangingConfiguration counter
    @Test
    fun `verify lifecycleOwner handles orientation changes`() {
        val observer = mockk<DefaultLifecycleObserver>(relaxUnitFun = true)
        mapboxNavigationApp.lifecycleOwner.lifecycle.addObserver(observer)

        // An activity is created and resumed.
        val (activity, lifecycle) = mockActivityLifecycle()
        mapboxNavigationApp.attach(activity)
        lifecycle.currentState = Lifecycle.State.RESUMED

        // The activity is destroyed and brought back to the resumed state.
        every { activity.isChangingConfigurations } returns true
        lifecycle.currentState = Lifecycle.State.DESTROYED
        val (activityNew, lifecycleNew) = mockActivityLifecycle()
        mapboxNavigationApp.attach(activityNew)
        lifecycleNew.currentState = Lifecycle.State.RESUMED

        // Verify this lifecycle observer will not receive destruction events.
        verify(exactly = 1) { observer.onCreate(any()) }
        verify(exactly = 1) { observer.onStart(any()) }
        verify(exactly = 1) { observer.onResume(any()) }
        verify(exactly = 0) { observer.onPause(any()) }
        verify(exactly = 0) { observer.onStop(any()) }
        verify(exactly = 0) { observer.onDestroy(any()) }
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

    private fun mockActivityLifecycle(): Pair<ComponentActivity, LifecycleRegistry> {
        val activity = mockk<ComponentActivity> {
            every { isChangingConfigurations } returns false
        }
        val lifecycle = LifecycleRegistry(activity)
            .also { it.currentState = Lifecycle.State.INITIALIZED }
        every { activity.lifecycle } returns lifecycle
        return Pair(activity, lifecycle)
    }
}
