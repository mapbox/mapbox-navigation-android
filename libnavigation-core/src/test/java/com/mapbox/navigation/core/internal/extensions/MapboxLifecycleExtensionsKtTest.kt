package com.mapbox.navigation.core.internal.extensions

import androidx.lifecycle.Lifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.testutil.TestLifecycleOwner
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxLifecycleExtensionsKtTest {

    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val testObserver = mockk<MapboxNavigationObserver>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.registerObserver(any()) } answers {
            firstArg<MapboxNavigationObserver>().onAttached(mapboxNavigation)
            MapboxNavigationApp
        }
        every { MapboxNavigationApp.unregisterObserver(any()) } answers {
            firstArg<MapboxNavigationObserver>().onDetached(mapboxNavigation)
            MapboxNavigationApp
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `LifecycleOwner attachCreated attaches in ON_CREATE detaches in ON_DESTROY`() {
        val lifecycleOwner = TestLifecycleOwner()

        lifecycleOwner.attachCreated(testObserver)
        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        lifecycleOwner.moveToState(Lifecycle.State.DESTROYED)

        verifyOrder {
            testObserver.onAttached(mapboxNavigation)
            testObserver.onDetached(mapboxNavigation)
        }
    }

    @Test
    fun `LifecycleOwner attachCreated does not attach without CREATED`() {
        val lifecycleOwner = TestLifecycleOwner()

        lifecycleOwner.attachCreated(testObserver)

        verify(exactly = 0) {
            testObserver.onAttached(any())
            testObserver.onDetached(any())
        }
    }

    @Test
    fun `LifecycleOwner attachStarted attaches in ON_START detaches in ON_STOP`() {
        val lifecycleOwner = TestLifecycleOwner()

        lifecycleOwner.attachStarted(testObserver)
        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        lifecycleOwner.moveToState(Lifecycle.State.CREATED)

        verifyOrder {
            testObserver.onAttached(mapboxNavigation)
            testObserver.onDetached(mapboxNavigation)
        }
    }

    @Test
    fun `LifecycleOwner attachStarted does not attach without STARTED`() {
        val lifecycleOwner = TestLifecycleOwner()

        lifecycleOwner.attachStarted(testObserver)
        lifecycleOwner.moveToState(Lifecycle.State.CREATED)

        verify(exactly = 0) {
            testObserver.onAttached(any())
            testObserver.onDetached(any())
        }
    }

    @Test
    fun `LifecycleOwner attachResumed attaches in ON_RESUME detaches in ON_PAUSE`() {
        val lifecycleOwner = TestLifecycleOwner()

        lifecycleOwner.attachResumed(testObserver)
        lifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        lifecycleOwner.moveToState(Lifecycle.State.STARTED)

        verifyOrder {
            testObserver.onAttached(mapboxNavigation)
            testObserver.onDetached(mapboxNavigation)
        }
    }

    @Test
    fun `LifecycleOwner attachResumed does not attach without RESUMED`() {
        val lifecycleOwner = TestLifecycleOwner()

        lifecycleOwner.attachResumed(testObserver)
        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        lifecycleOwner.moveToState(Lifecycle.State.CREATED)

        verify(exactly = 0) {
            testObserver.onAttached(any())
            testObserver.onDetached(any())
        }
    }
}
