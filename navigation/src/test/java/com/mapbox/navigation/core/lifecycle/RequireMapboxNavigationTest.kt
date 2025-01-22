package com.mapbox.navigation.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RequireMapboxNavigationTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private class SystemUnderTest(
        onCreatedObserver: MapboxNavigationObserver? = null,
        onStartedObserver: MapboxNavigationObserver? = null,
        onResumedObserver: MapboxNavigationObserver? = null,
        onInitialize: (() -> Unit)? = null,
    ) : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry

        fun moveToState(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }

        val mapboxNavigation by requireMapboxNavigation(
            onCreatedObserver = onCreatedObserver,
            onStartedObserver = onStartedObserver,
            onResumedObserver = onResumedObserver,
            onInitialize = onInitialize,
        )
    }

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        mockkStatic(MapboxNavigationApp::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test(expected = IllegalStateException::class)
    fun `crashes before setup`() {
        SystemUnderTest().mapboxNavigation
    }

    @Test
    fun `mapboxNavigation is available when current is not null`() {
        every { MapboxNavigationApp.current() } returns mockk()

        SystemUnderTest().mapboxNavigation
    }

    @Test
    fun `mapboxNavigation is available after MapboxNavigationApp#setup and lifecycle is CREATED`() {
        mockMapboxNavigationAppBehavior()
        val sut = SystemUnderTest()

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)

        // This should not crash
        sut.mapboxNavigation
    }

    @Test
    fun `onInitialize is called before first reference to the delegate`() {
        mockMapboxNavigationAppBehavior()
        val onInitialize: (() -> Unit) = {
            // do nothing
        }

        val mockOnInitialize = spyk(onInitialize)
        val sut = SystemUnderTest(onInitialize = mockOnInitialize)

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)

        verify { mockOnInitialize.invoke() }
    }

    @Test
    fun `multiple delegate references are the same instance`() {
        mockMapboxNavigationAppBehavior()
        val sut = SystemUnderTest()

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)
        val firstRef = sut.mapboxNavigation
        val secondRef = sut.mapboxNavigation

        assertTrue(firstRef === secondRef)
    }

    @Test(expected = IllegalStateException::class)
    fun `delegate reference will crash if accessed after lifecycle is DESTROYED`() {
        mockMapboxNavigationAppBehavior()
        val sut = SystemUnderTest()

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)
        sut.mapboxNavigation
        sut.moveToState(Lifecycle.State.DESTROYED)

        // This should crash
        sut.mapboxNavigation
    }

    @Test
    fun `delegate references changes after MapboxNavigationApp is reset`() {
        mockMapboxNavigationAppBehavior()
        val sut = SystemUnderTest()

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)
        val firstRef = sut.mapboxNavigation
        MapboxNavigationApp.disable()
        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        val secondRef = sut.mapboxNavigation

        assertTrue(firstRef !== secondRef)
    }

    @Test(expected = IllegalStateException::class)
    fun `delegate reference will crash if accessed after MapboxNavigationApp#disable`() {
        mockMapboxNavigationAppBehavior()
        val sut = SystemUnderTest()

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)
        sut.mapboxNavigation
        MapboxNavigationApp.disable()

        // This should crash
        sut.mapboxNavigation
    }

    @Test
    fun `onInitialize can be used to setup the app`() {
        mockMapboxNavigationAppBehavior()

        val sut = SystemUnderTest {
            MapboxNavigationApp.setup(mockk<NavigationOptions>())
        }
        sut.moveToState(Lifecycle.State.CREATED)

        // This should not crash
        sut.mapboxNavigation
    }

    @Test
    fun `observer callback order of resetting MapboxNavigationApp`() {
        mockMapboxNavigationAppBehavior()
        val onCreatedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onStartedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onResumedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val sut = SystemUnderTest(
            onCreatedObserver = onCreatedObserver,
            onStartedObserver = onStartedObserver,
            onResumedObserver = onResumedObserver,
        )

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.RESUMED)
        val firstRef = sut.mapboxNavigation
        MapboxNavigationApp.disable()
        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        val secondRef = sut.mapboxNavigation

        verifyOrder {
            onCreatedObserver.onAttached(firstRef)
            onStartedObserver.onAttached(firstRef)
            onResumedObserver.onAttached(firstRef)
            onCreatedObserver.onDetached(firstRef)
            onStartedObserver.onDetached(firstRef)
            onResumedObserver.onDetached(firstRef)
            onCreatedObserver.onAttached(secondRef)
            onStartedObserver.onAttached(secondRef)
            onResumedObserver.onAttached(secondRef)
        }
    }

    @Test
    fun `CREATED lifecycle state callback order`() {
        mockMapboxNavigationAppBehavior()
        val onCreatedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onStartedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onResumedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val sut = SystemUnderTest(
            onCreatedObserver = onCreatedObserver,
            onStartedObserver = onStartedObserver,
            onResumedObserver = onResumedObserver,
        )

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.CREATED)
        val firstRef = sut.mapboxNavigation
        sut.moveToState(Lifecycle.State.DESTROYED)

        verifyOrder {
            onCreatedObserver.onAttached(firstRef)
            onCreatedObserver.onDetached(firstRef)
        }
        verify(exactly = 0) {
            onStartedObserver.onDetached(any())
            onStartedObserver.onDetached(any())
            onResumedObserver.onDetached(any())
            onResumedObserver.onDetached(any())
        }
    }

    @Test
    fun `STARTED lifecycle state callback order`() {
        mockMapboxNavigationAppBehavior()
        val onCreatedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onStartedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onResumedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val sut = SystemUnderTest(
            onCreatedObserver = onCreatedObserver,
            onStartedObserver = onStartedObserver,
            onResumedObserver = onResumedObserver,
        )

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.STARTED)
        val firstRef = sut.mapboxNavigation
        sut.moveToState(Lifecycle.State.CREATED)

        verifyOrder {
            onCreatedObserver.onAttached(firstRef)
            onStartedObserver.onAttached(firstRef)
            onStartedObserver.onDetached(firstRef)
        }
        verify(exactly = 0) {
            onCreatedObserver.onDetached(any())
            onResumedObserver.onAttached(any())
            onResumedObserver.onDetached(any())
        }
    }

    @Test
    fun `RESUMED lifecycle state callback order`() {
        mockMapboxNavigationAppBehavior()
        val onCreatedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onStartedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val onResumedObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val sut = SystemUnderTest(
            onCreatedObserver = onCreatedObserver,
            onStartedObserver = onStartedObserver,
            onResumedObserver = onResumedObserver,
        )

        MapboxNavigationApp.setup(mockk<NavigationOptions>())
        sut.moveToState(Lifecycle.State.RESUMED)
        val firstRef = sut.mapboxNavigation
        sut.moveToState(Lifecycle.State.STARTED)

        verifyOrder {
            onCreatedObserver.onAttached(firstRef)
            onStartedObserver.onAttached(firstRef)
            onResumedObserver.onAttached(firstRef)
            onResumedObserver.onDetached(firstRef)
        }
        verify(exactly = 0) {
            onCreatedObserver.onDetached(any())
            onStartedObserver.onDetached(any())
        }
    }

    /**
     * This mocks the behavior of [MapboxNavigationApp] in order to showcase what is expected from
     * the [RequireMapboxNavigationDelegate].
     */
    private fun mockMapboxNavigationAppBehavior() {
        var mockMapboxNavigation: MapboxNavigation? = null
        var isSetup = false
        val registeredMapboxNavigationObservers = mutableListOf<MapboxNavigationObserver>()
        val attachedLifecycleOwner = mutableListOf<LifecycleOwner>()

        every { MapboxNavigationApp.current() } answers {
            mockMapboxNavigation
        }
        every { MapboxNavigationApp.disable() } answers {
            registeredMapboxNavigationObservers.forEach { it.onDetached(mockMapboxNavigation!!) }
            mockMapboxNavigation = null
            isSetup = false
            MapboxNavigationApp
        }
        every { MapboxNavigationApp.attach(any()) } answers {
            attachedLifecycleOwner.add(firstArg())
            firstArg<LifecycleOwner>().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        if (isSetup) {
                            mockMapboxNavigation = mockk()
                            registeredMapboxNavigationObservers.forEach { observer ->
                                observer.onAttached(mockMapboxNavigation!!)
                            }
                        }
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        super.onDestroy(owner)
                        attachedLifecycleOwner.remove(firstArg())
                        val isCreated = attachedLifecycleOwner.any {
                            it.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
                        }
                        if (!isCreated) {
                            registeredMapboxNavigationObservers.forEach { observer ->
                                observer.onDetached(mockMapboxNavigation!!)
                            }
                            mockMapboxNavigation = null
                        }
                    }
                },
            )
            MapboxNavigationApp
        }
        every { MapboxNavigationApp.setup(any<NavigationOptions>()) } answers {
            isSetup = true
            val isCreated = attachedLifecycleOwner.any {
                it.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
            }
            if (isCreated) {
                mockMapboxNavigation = mockk()
                registeredMapboxNavigationObservers.forEach { observer ->
                    observer.onAttached(mockMapboxNavigation!!)
                }
            }
            MapboxNavigationApp
        }
        every {
            MapboxNavigationApp.registerObserver(any())
        } answers {
            registeredMapboxNavigationObservers.add(firstArg())
            firstArg<MapboxNavigationObserver>().onAttached(mockMapboxNavigation!!)
            MapboxNavigationApp
        }
        every {
            MapboxNavigationApp.unregisterObserver(any())
        } answers {
            registeredMapboxNavigationObservers.remove(firstArg())
            firstArg<MapboxNavigationObserver>().onDetached(mockMapboxNavigation!!)
            MapboxNavigationApp
        }
    }
}
