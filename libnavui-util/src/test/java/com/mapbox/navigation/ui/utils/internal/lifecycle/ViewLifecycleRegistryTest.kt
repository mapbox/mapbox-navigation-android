package com.mapbox.navigation.ui.utils.internal.lifecycle

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewLifecycleRegistryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        mockkStatic(ViewTreeLifecycleOwner::class)
    }

    @Test
    fun `mirrors hosting lifecycles state when attached`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(hostingLifecycleOwner)
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        localLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        localLifecycleOwner.attach()
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        localLifecycleOwner.detach()

        verifyOrder {
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_CREATE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_START)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_RESUME)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_STOP)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        }
    }

    @Test
    fun `stops when detached and hosting lifecycle resumed`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(hostingLifecycleOwner)
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        localLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        localLifecycleOwner.attach()
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        localLifecycleOwner.detach()

        verifyOrder {
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_CREATE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_START)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_RESUME)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_STOP)
        }
    }

    @Test
    fun `stopped when detached and hosting lifecycle stopped`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(hostingLifecycleOwner)
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        localLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        localLifecycleOwner.attach()
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        localLifecycleOwner.detach()

        verifyOrder {
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_CREATE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_START)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_RESUME)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_STOP)
        }
    }

    @Test
    fun `hosting lifecycle destroyed but view never attached, remain initialized only`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(hostingLifecycleOwner)
        val viewLifecycleRegistry = localLifecycleOwner.viewLifecycleRegistry
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        viewLifecycleRegistry.addObserver(lifecycleObserver)
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(exactly = 0) {
            lifecycleObserver.onStateChanged(any(), any())
        }
        Assert.assertTrue(localLifecycleOwner.lifecycle.currentState == Lifecycle.State.INITIALIZED)
    }

    @Test
    fun `destroyed state delivered even if view is detached`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(hostingLifecycleOwner)
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        localLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        localLifecycleOwner.attach()
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        localLifecycleOwner.detach()
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verifyOrder {
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_CREATE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_START)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_RESUME)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_STOP)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        }
    }

    @Test
    fun `detaching view when hosting lifecycle is only initialized has no effect`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(hostingLifecycleOwner)
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        localLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        localLifecycleOwner.attach()
        localLifecycleOwner.detach()

        verify(exactly = 0) {
            lifecycleObserver.onStateChanged(any(), any())
        }
        Assert.assertTrue(localLifecycleOwner.lifecycle.currentState == Lifecycle.State.INITIALIZED)
    }

    @Test
    fun `view is already attached on lifecycle creation, handle state correctly`() {
        val hostingLifecycleOwner = generateHostingLifecycleOwner()
        val localLifecycleOwner = generateLocalLifecycleOwner(
            hostingLifecycleOwner,
            initializeAttached = true,
        )
        val lifecycleObserver = mockk<LifecycleEventObserver>(relaxUnitFun = true)

        localLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        hostingLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verifyOrder {
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_CREATE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_START)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_RESUME)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_PAUSE)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_STOP)
            lifecycleObserver.onStateChanged(localLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(ViewTreeLifecycleOwner::class)
    }

    private fun generateHostingLifecycleOwner() = TestLifecycleOwner(
        initialState = Lifecycle.State.INITIALIZED,
    )

    private fun generateLocalLifecycleOwner(
        hostingLifecycleOwner: LifecycleOwner,
        initializeAttached: Boolean = false,
    ) = DummyLifecycleOwner(
        hostingLifecycleOwner,
        initializeAttached,
    )
}

private class DummyLifecycleOwner(
    hostingLifecycleOwner: LifecycleOwner,
    initializeAttached: Boolean,
) : LifecycleOwner {

    private val attachStateSlot = slot<View.OnAttachStateChangeListener>()

    val view = mockk<View>(relaxUnitFun = true) {
        every { addOnAttachStateChangeListener(capture(attachStateSlot)) } just Runs
        every { isAttachedToWindow } returns initializeAttached
    }
    val viewLifecycleRegistry: ViewLifecycleRegistry

    init {
        every { ViewTreeLifecycleOwner.get(view) } returns hostingLifecycleOwner
        viewLifecycleRegistry = ViewLifecycleRegistry(
            localLifecycleOwner = this,
            view = view,
        )
    }

    fun attach() {
        attachStateSlot.captured.onViewAttachedToWindow(view)
        every { view.isAttachedToWindow } returns true
    }

    fun detach() {
        attachStateSlot.captured.onViewDetachedFromWindow(view)
        every { view.isAttachedToWindow } returns false
    }

    override fun getLifecycle(): Lifecycle = viewLifecycleRegistry
}
