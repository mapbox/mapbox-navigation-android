package com.mapbox.navigation.core.internal.extensions

import androidx.lifecycle.Lifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.testutil.TestLifecycleOwner
import com.mapbox.navigation.core.testutil.TestMapboxNavigationObserver
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxLifecycleExtensionsKtTest {

    @Test
    fun `LifecycleOwner attachOnLifecycle ON_CREATE - ON_DESTROY`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = TestMapboxNavigationObserver()

        lifecycleOwner.attachOnLifecycle(
            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_DESTROY,
            mapboxNavigation,
            testObserver
        )

        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        assertEquals(mapboxNavigation, testObserver.attachedTo)
        lifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
        assertEquals(null, testObserver.attachedTo)
    }

    @Test
    fun `LifecycleOwner attachOnLifecycle ON_START - ON_STOP`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = TestMapboxNavigationObserver()

        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        lifecycleOwner.attachOnLifecycle(
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_STOP,
            mapboxNavigation,
            testObserver
        )

        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        assertEquals(mapboxNavigation, testObserver.attachedTo)
        lifecycleOwner.moveToState(Lifecycle.State.CREATED)
        assertEquals(null, testObserver.attachedTo)
    }

    @Test
    fun `LifecycleOwner attachOnLifecycle ON_RESUME - ON_PAUSE`() {
        val lifecycleOwner = TestLifecycleOwner()
        val mapboxNavigation = mockk<MapboxNavigation>()
        val testObserver = TestMapboxNavigationObserver()

        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        lifecycleOwner.attachOnLifecycle(
            Lifecycle.Event.ON_RESUME,
            Lifecycle.Event.ON_PAUSE,
            mapboxNavigation,
            testObserver
        )

        lifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        assertEquals(mapboxNavigation, testObserver.attachedTo)
        lifecycleOwner.moveToState(Lifecycle.State.STARTED)
        assertEquals(null, testObserver.attachedTo)
    }
}
