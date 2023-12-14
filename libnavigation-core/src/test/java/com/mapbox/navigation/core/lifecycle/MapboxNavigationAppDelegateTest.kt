@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import androidx.core.app.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.internal.lifecycle.CarAppLifecycleOwnerTest
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.DefaultLifecycleObserver
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalPreviewMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
class MapboxNavigationAppDelegateTest {
    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxNavigation: MapboxNavigation = mockk()
    private val navigationOptions: NavigationOptions = mockk {
        every { accessToken } returns "test_access_token"
    }

    private val mapboxNavigationApp = MapboxNavigationAppDelegate()

    @Before
    fun setup() {
        mockkStatic(MapboxNavigationProvider::class)
        every { MapboxNavigationProvider.create(navigationOptions) } returns mapboxNavigation
        every { MapboxNavigationProvider.retrieve() } returns mapboxNavigation
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `verify onAttached and onDetached when multiple lifecycles have started`() {
        mapboxNavigationApp.setup { navigationOptions }

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
        mapboxNavigationApp.setup { navigationOptions }

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
        mapboxNavigationApp.setup { navigationOptions }

        verify(exactly = 1) { firstObserver.onAttached(any()) }
        verify(exactly = 1) { secondObserver.onAttached(any()) }
        verify(exactly = 0) { firstObserver.onDetached(any()) }
        verify(exactly = 0) { secondObserver.onDetached(any()) }
    }

    @Test
    fun `verify setup will detach old and attach a new`() {
        val attachedSlot = mutableListOf<MapboxNavigation>()
        val detachedSlot = mutableListOf<MapboxNavigation>()
        val observer = mockk<MapboxNavigationObserver> {
            every { onAttached(capture(attachedSlot)) } just runs
            every { onDetached(capture(detachedSlot)) } just runs
        }
        mapboxNavigationApp.registerObserver(observer)

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        mapboxNavigationApp.setup { navigationOptions }
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup { navigationOptions }

        assertEquals(2, attachedSlot.size)
        assertEquals(1, detachedSlot.size)
        assertEquals(attachedSlot[0], detachedSlot[0])
        verifyOrder {
            observer.onAttached(attachedSlot[0])
            observer.onDetached(attachedSlot[0])
            observer.onAttached(attachedSlot[1])
        }
    }

    @Test
    fun `verify setup will not recreate mapboxNavigation when undergoing orientation change`() {
        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(firstObserver)
        mapboxNavigationApp.registerObserver(secondObserver)

        val (portraitActivity, portraitLifecycle) = mockActivityLifecycle()
        portraitLifecycle.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup { navigationOptions }
        mapboxNavigationApp.attach(portraitActivity)
        every { portraitActivity.isChangingConfigurations } returns true
        portraitLifecycle.currentState = Lifecycle.State.DESTROYED

        val (landscapeActivity, landscapeLifecycle) = mockActivityLifecycle()
        landscapeLifecycle.currentState = Lifecycle.State.RESUMED
        every { landscapeActivity.isChangingConfigurations } returns false
        landscapeLifecycle.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.setup { navigationOptions }
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

        val observer = spyk(object : DefaultLifecycleObserver() {})
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
        val observer = spyk(object : DefaultLifecycleObserver() {})
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
        mapboxNavigationApp.setup { navigationOptions }

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
        mapboxNavigationApp.setup { navigationOptions }

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
        mapboxNavigationApp.setup { navigationOptions }

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
    fun `verify disable will detach and current becomes null`() {
        mapboxNavigationApp.setup { navigationOptions }

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)
        mapboxNavigationApp.disable()
        mapboxNavigationApp.unregisterObserver(observer)

        assertNull(MapboxNavigationApp.current())
        verify(exactly = 1) { observer.onDetached(any()) }
    }

    @Test
    fun `verify current is null when all lifecycle owners are destroyed`() {
        mapboxNavigationApp.setup { navigationOptions }

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
        mapboxNavigationApp.setup { navigationOptions }

        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        mapboxNavigationApp.attach(testLifecycleOwner)
        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)

        assertNull(mapboxNavigationApp.current())
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        assertNotNull(mapboxNavigationApp.current())
    }

    @Test
    fun `verify MapboxNavigationObserver lifecycles are called once`() {
        mapboxNavigationApp.setup { navigationOptions }
        val testLifecycleOwner = CarAppLifecycleOwnerTest.TestLifecycleOwner()
        testLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        mapboxNavigationApp.attach(testLifecycleOwner)

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationApp.registerObserver(observer)
        mapboxNavigationApp.registerObserver(observer)
        mapboxNavigationApp.unregisterObserver(observer)
        mapboxNavigationApp.unregisterObserver(observer)

        verify(exactly = 1) { observer.onAttached(any()) }
        verify(exactly = 1) { observer.onDetached(any()) }
    }

    @Test
    fun `verify getObserver will return the registered observer`() {
        val observer = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observer)

        val retrieved = mapboxNavigationApp.getObserver(ExampleObserverA::class)
        assertEquals(observer, retrieved)
    }

    @Test
    fun `verify getObserver will return first registered observer`() {
        val observerFirst = ExampleObserverA()
        val observerSecond = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observerFirst)
        mapboxNavigationApp.registerObserver(observerSecond)

        val retrieved = mapboxNavigationApp.getObserver(ExampleObserverA::class)
        assertEquals(observerFirst, retrieved)
    }

    @Test
    fun `verify getObserver will return the same registered class`() {
        val observerFirst = ExampleObserverA()
        val observerSecond = ExampleObserverB()
        mapboxNavigationApp.registerObserver(observerFirst)
        mapboxNavigationApp.registerObserver(observerSecond)

        val retrieved = mapboxNavigationApp.getObserver(ExampleObserverB::class)
        assertEquals(observerSecond, retrieved)
    }

    @Test(expected = IllegalStateException::class)
    fun `verify getObserver will crash when the observer is not registered`() {
        val observer = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observer)

        // Should crash
        mapboxNavigationApp.getObserver(ExampleObserverB::class)
    }

    @Test(expected = IllegalStateException::class)
    fun `verify getObserver will crash when the observer is removed`() {
        val observerFirst = ExampleObserverA()
        val observerSecond = ExampleObserverB()
        mapboxNavigationApp.registerObserver(observerFirst)
        mapboxNavigationApp.registerObserver(observerSecond)
        mapboxNavigationApp.unregisterObserver(observerFirst)

        // Should crash
        mapboxNavigationApp.getObserver(ExampleObserverA::class)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `verify getObserver java will crash when the observer is not registered`() {
        val observer = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observer)

        // Should crash
        mapboxNavigationApp.getObserver(ExampleObserverB::class.java)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `verify getObserver java will crash when the observer is removed`() {
        val observerFirst = ExampleObserverA()
        val observerSecond = ExampleObserverB()
        mapboxNavigationApp.registerObserver(observerFirst)
        mapboxNavigationApp.registerObserver(observerSecond)
        mapboxNavigationApp.unregisterObserver(observerFirst)

        // Should crash
        mapboxNavigationApp.getObserver(ExampleObserverA::class.java)
    }

    @Test
    fun `verify getObservers will return the registered observer`() {
        val observer = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observer)

        val retrieved = mapboxNavigationApp.getObservers(ExampleObserverA::class)
        val retrievedJava = mapboxNavigationApp.getObservers(ExampleObserverA::class.java)
        assertEquals(1, retrieved.size)
        assertEquals(observer, retrieved[0])
        assertEquals(retrieved, retrievedJava)
    }

    @Test
    fun `verify getObservers will return all registered observers of the same type`() {
        val observerFirst = ExampleObserverA()
        val observerSecond = ExampleObserverB()
        val observerThird = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observerFirst)
        mapboxNavigationApp.registerObserver(observerSecond)
        mapboxNavigationApp.registerObserver(observerThird)

        val retrieved = mapboxNavigationApp.getObservers(ExampleObserverA::class)
        val retrievedJava = mapboxNavigationApp.getObservers(ExampleObserverA::class.java)
        assertEquals(2, retrieved.size)
        assertEquals(observerFirst, retrieved[0])
        assertEquals(observerThird, retrieved[1])
        assertEquals(retrieved, retrievedJava)
    }

    fun `verify getObservers will return empty when observer is not registered`() {
        val observer = ExampleObserverA()
        mapboxNavigationApp.registerObserver(observer)

        val retrieved = mapboxNavigationApp.getObservers(ExampleObserverB::class)
        val retrievedJava = mapboxNavigationApp.getObservers(ExampleObserverB::class.java)
        assertTrue(retrieved.isEmpty())
        assertEquals(retrieved, retrievedJava)
    }

    fun `verify getObservers will return empty when observers are removed`() {
        val observerFirst = ExampleObserverA()
        val observerSecond = ExampleObserverB()
        mapboxNavigationApp.registerObserver(observerFirst)
        mapboxNavigationApp.registerObserver(observerSecond)
        mapboxNavigationApp.unregisterObserver(observerFirst)

        // Should crash
        val retrieved = mapboxNavigationApp.getObservers(ExampleObserverA::class)
        val retrievedJava = mapboxNavigationApp.getObservers(ExampleObserverA::class.java)
        assertTrue(retrieved.isEmpty())
        assertEquals(retrieved, retrievedJava)
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

    /**
     * Used for the [MapboxNavigationApp.getObserver] tests because they require a class definition.
     */
    private class ExampleObserverA : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            // no op
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            // no op
        }
    }

    /**
     * Used for the [MapboxNavigationApp.getObserver] tests because they require a class definition.
     */
    private class ExampleObserverB : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            // no op
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            // no op
        }
    }
}
