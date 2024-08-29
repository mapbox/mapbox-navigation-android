package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.GetLifecycleStateCallback
import com.mapbox.common.LifecycleMonitoringState
import com.mapbox.common.LifecycleObserver
import com.mapbox.common.LifecycleState
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class LifecycleStateProviderTest {

    private val getLifecycleStateCancelable = mockk<Cancelable>(relaxed = true)
    private val lifecycleMonitor = mockk<LifecycleMonitorCancelableWrapper>(relaxed = true) {
        every { getLifecycleState(any()) } returns getLifecycleStateCancelable
    }
    private val provider = LifecycleStateProvider { lifecycleMonitor }

    @Test
    fun initialValueIsUnknown() {
        assertEquals(LifecycleState.UNKNOWN, provider.currentState)
    }

    @Test
    fun stateIsChanged() {
        val observerSlot = slot<LifecycleObserver>()
        provider.init()

        verify { lifecycleMonitor.registerObserver(capture(observerSlot)) }

        assertEquals(LifecycleState.UNKNOWN, provider.currentState)

        observerSlot.captured.onLifecycleStateChanged(LifecycleState.BACKGROUND)

        assertEquals(LifecycleState.BACKGROUND, provider.currentState)
    }

    @Test
    fun stateIsResetToUnknownWhenMonitoringStateIsStopped() {
        val observerSlot = slot<LifecycleObserver>()
        provider.init()
        verify { lifecycleMonitor.registerObserver(capture(observerSlot)) }
        observerSlot.captured.onLifecycleStateChanged(LifecycleState.BACKGROUND)

        observerSlot.captured.onMonitoringStateChanged(LifecycleMonitoringState.STOPPED, null)

        assertEquals(LifecycleState.UNKNOWN, provider.currentState)
    }

    @Test
    fun destroy() {
        val observerSlot = slot<LifecycleObserver>()
        provider.init()
        verify { lifecycleMonitor.registerObserver(capture(observerSlot)) }
        observerSlot.captured.onLifecycleStateChanged(LifecycleState.BACKGROUND)

        provider.destroy()

        verify { lifecycleMonitor.unregisterObserver(observerSlot.captured) }
        assertEquals(LifecycleState.UNKNOWN, provider.currentState)
    }

    @Test
    fun getInitialStateOnInitReturnsError() {
        val callbackSlot = slot<GetLifecycleStateCallback>()
        provider.init()

        verify { lifecycleMonitor.getLifecycleState(capture(callbackSlot)) }

        assertEquals(LifecycleState.UNKNOWN, provider.currentState)

        callbackSlot.captured.run(ExpectedFactory.createError("Some error"))

        assertEquals(LifecycleState.UNKNOWN, provider.currentState)
    }

    @Test
    fun getInitialStateOnInitReturnsValue() {
        val callbackSlot = slot<GetLifecycleStateCallback>()
        provider.init()

        verify { lifecycleMonitor.getLifecycleState(capture(callbackSlot)) }

        assertEquals(LifecycleState.UNKNOWN, provider.currentState)

        callbackSlot.captured.run(ExpectedFactory.createValue(LifecycleState.BACKGROUND))

        assertEquals(LifecycleState.BACKGROUND, provider.currentState)
    }

    @Test
    fun getInitialStateInCancelledWhenObserverFires() {
        val observerSlot = slot<LifecycleObserver>()
        provider.init()
        verify { lifecycleMonitor.registerObserver(capture(observerSlot)) }

        observerSlot.captured.onLifecycleStateChanged(LifecycleState.BACKGROUND)

        verify { getLifecycleStateCancelable.cancel() }
    }

    @Test
    fun getInitialStateInCancelledWhenMonitoringStateIsStopped() {
        val observerSlot = slot<LifecycleObserver>()
        provider.init()
        verify { lifecycleMonitor.registerObserver(capture(observerSlot)) }

        observerSlot.captured.onMonitoringStateChanged(LifecycleMonitoringState.STOPPED, null)

        verify { getLifecycleStateCancelable.cancel() }
    }

    @Test
    fun getInitialStateInCancelledOnDestroy() {
        provider.init()

        provider.destroy()

        verify { getLifecycleStateCancelable.cancel() }
    }

    @Test
    fun getInitialStateInNotCancelledTwice() {
        val observerSlot = slot<LifecycleObserver>()
        provider.init()
        verify { lifecycleMonitor.registerObserver(capture(observerSlot)) }
        observerSlot.captured.onLifecycleStateChanged(LifecycleState.BACKGROUND)
        clearAllMocks(answers = false)

        provider.destroy()

        verify(exactly = 0) { getLifecycleStateCancelable.cancel() }
    }
}
