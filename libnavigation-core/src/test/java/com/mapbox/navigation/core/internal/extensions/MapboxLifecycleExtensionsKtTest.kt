package com.mapbox.navigation.core.internal.extensions

import androidx.lifecycle.Lifecycle
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.testutil.TestLifecycleOwner
import com.mapbox.navigation.core.testutil.TestMapboxNavigationObserver
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxLifecycleExtensionsKtTest {

    @Test
    fun `LifecycleOwner attachCreated attaches in ON_CREATE detaches in ON_DESTROY`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = TestMapboxNavigationObserver()

        lifecycleOwner.attachCreated(
            mapboxNavigation,
            testObserver,
        )

        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        assertEquals(mapboxNavigation, testObserver.attachedTo)
        lifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
        assertEquals(null, testObserver.attachedTo)
    }

    @Test
    fun `LifecycleOwner attachStarted attaches in ON_START detaches in ON_STOP`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = TestMapboxNavigationObserver()

        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        lifecycleOwner.attachStarted(
            mapboxNavigation,
            testObserver,
        )

        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        assertEquals(mapboxNavigation, testObserver.attachedTo)
        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        assertEquals(null, testObserver.attachedTo)
    }

    @Test
    fun `LifecycleOwner attachResumed attaches in ON_RESUME detaches in ON_PAUSE`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = TestMapboxNavigationObserver()

        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        lifecycleOwner.attachResumed(
            mapboxNavigation,
            testObserver,
        )

        lifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        assertEquals(mapboxNavigation, testObserver.attachedTo)
        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        assertEquals(null, testObserver.attachedTo)
    }

    @Test
    fun `LifecycleOwner attachOnLifecycle attaches and detaches once`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = spyk(TestMapboxNavigationObserver())

        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        lifecycleOwner.attachOnLifecycle(
            Lifecycle.Event.ON_RESUME,
            Lifecycle.Event.ON_PAUSE,
            mapboxNavigation,
            testObserver,
        )

        lifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        verify(exactly = 1) { testObserver.onAttached(mapboxNavigation) }
        lifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
        verify(exactly = 1) { testObserver.onDetached(mapboxNavigation) }
    }
}
