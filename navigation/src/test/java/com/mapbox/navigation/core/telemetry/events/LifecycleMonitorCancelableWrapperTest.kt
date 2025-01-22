package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.GetLifecycleStateCallback
import com.mapbox.common.LifecycleMonitorInterface
import com.mapbox.common.LifecycleObserver
import com.mapbox.common.LifecycleState
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

internal class LifecycleMonitorCancelableWrapperTest {

    private val callback = mockk<GetLifecycleStateCallback>(relaxed = true)
    private val observer = mockk<LifecycleObserver>()
    private val lifecycleMonitorInterface = mockk<LifecycleMonitorInterface>(relaxed = true)
    private val wrapper = LifecycleMonitorCancelableWrapper(lifecycleMonitorInterface)

    @Test
    fun registerObserver() {
        wrapper.registerObserver(observer)

        verify { lifecycleMonitorInterface.registerObserver(observer) }
    }

    @Test
    fun unregisterObserver() {
        wrapper.unregisterObserver(observer)

        verify { lifecycleMonitorInterface.unregisterObserver(observer) }
    }

    @Test
    fun getLifecycleStateNotCancelled() {
        val result = ExpectedFactory.createValue<String, LifecycleState>(LifecycleState.FOREGROUND)
        val callbackWrapperSlot = slot<GetLifecycleStateCallback>()
        val cancellable = wrapper.getLifecycleState(callback)

        verify { lifecycleMonitorInterface.getLifecycleState(capture(callbackWrapperSlot)) }

        callbackWrapperSlot.captured.run(result)

        verify { callback.run(result) }
    }

    @Test
    fun getLifecycleStateCancelled() {
        val result = ExpectedFactory.createValue<String, LifecycleState>(LifecycleState.FOREGROUND)
        val callbackWrapperSlot = slot<GetLifecycleStateCallback>()
        val cancellable = wrapper.getLifecycleState(callback)

        verify { lifecycleMonitorInterface.getLifecycleState(capture(callbackWrapperSlot)) }

        cancellable.cancel()

        callbackWrapperSlot.captured.run(result)

        verify(exactly = 0) { callback.run(any()) }
    }

    @Test
    fun getLifecycleStateMultipleRunsSimultaneously() {
        val result = ExpectedFactory.createValue<String, LifecycleState>(LifecycleState.FOREGROUND)
        val callback1WrapperSlot = slot<GetLifecycleStateCallback>()
        val callback2WrapperSlot = slot<GetLifecycleStateCallback>()

        val callback2 = mockk<GetLifecycleStateCallback>(relaxed = true)

        val cancellable1 = wrapper.getLifecycleState(callback)
        verify { lifecycleMonitorInterface.getLifecycleState(capture(callback1WrapperSlot)) }
        clearAllMocks(answers = false)

        val cancellable2 = wrapper.getLifecycleState(callback2)
        verify { lifecycleMonitorInterface.getLifecycleState(capture(callback2WrapperSlot)) }

        cancellable2.cancel()

        callback1WrapperSlot.captured.run(result)
        callback2WrapperSlot.captured.run(result)

        verify { callback.run(result) }
        verify(exactly = 0) { callback2.run(result) }
    }
}
