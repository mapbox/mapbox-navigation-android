package com.mapbox.navigation.core.trip.session

import android.app.PendingIntent
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

internal class CancellableLocationEngineTest {

    private val originalEngine = mockk<LocationEngine>(relaxed = true)
    private val wrapper = CancellableLocationEngine(originalEngine)
    private val callback = mockk<LocationEngineCallback<LocationEngineResult>>(relaxed = true)

    @Test
    fun requestLocationUpdates() {
        val request = mockk<LocationEngineRequest>()
        val looper = mockk<Looper>()

        wrapper.requestLocationUpdates(request, callback, looper)

        verify(exactly = 1) { originalEngine.requestLocationUpdates(request, callback, looper) }
    }

    @Test
    fun requestLocationUpdatesWithIntent() {
        val request = mockk<LocationEngineRequest>()
        val intent = mockk<PendingIntent>()

        wrapper.requestLocationUpdates(request, intent)

        verify(exactly = 1) { originalEngine.requestLocationUpdates(request, intent) }
    }

    @Test
    fun removeLocationUpdates() {
        wrapper.removeLocationUpdates(callback)

        verify(exactly = 1) { originalEngine.removeLocationUpdates(callback) }
    }

    @Test
    fun removeLocationUpdatesWithIntent() {
        val intent = mockk<PendingIntent>()

        wrapper.removeLocationUpdates(intent)

        verify(exactly = 1) { originalEngine.removeLocationUpdates(intent) }
    }

    @Test
    fun getLastLocationSuccess() {
        val result = mockk<LocationEngineResult>()
        val callbackCaptor = slot<LocationEngineCallback<LocationEngineResult>>()

        wrapper.getLastLocation(callback)

        verify(exactly = 1) {
            originalEngine.getLastLocation(capture(callbackCaptor))
        }
        callbackCaptor.captured.onSuccess(result)

        verify(exactly = 1) { callback.onSuccess(result) }
    }

    @Test
    fun getLastLocationFailure() {
        val exception = IllegalArgumentException()
        val callbackCaptor = slot<LocationEngineCallback<LocationEngineResult>>()

        wrapper.getLastLocation(callback)

        verify(exactly = 1) {
            originalEngine.getLastLocation(capture(callbackCaptor))
        }
        callbackCaptor.captured.onFailure(exception)

        verify(exactly = 1) { callback.onFailure(exception) }
    }

    @Test
    fun getLastLocationSuccessAfterCancel() {
        val result = mockk<LocationEngineResult>()
        val callbackCaptor = slot<LocationEngineCallback<LocationEngineResult>>()

        wrapper.getLastLocation(callback)

        verify(exactly = 1) {
            originalEngine.getLastLocation(capture(callbackCaptor))
        }
        wrapper.cancelLastLocationTask(callback)

        callbackCaptor.captured.onSuccess(result)

        verify(exactly = 0) { callback.onSuccess(any()) }
    }

    @Test
    fun getLastLocationFailureAfterCancel() {
        val exception = IllegalArgumentException()
        val callbackCaptor = slot<LocationEngineCallback<LocationEngineResult>>()

        wrapper.getLastLocation(callback)

        verify(exactly = 1) {
            originalEngine.getLastLocation(capture(callbackCaptor))
        }
        wrapper.cancelLastLocationTask(callback)

        callbackCaptor.captured.onFailure(exception)

        verify(exactly = 0) { callback.onFailure(any()) }
    }

    @Test
    fun getLastLocationSuccessAfterCancellingAnotherTask() {
        val secondCallback = mockk<LocationEngineCallback<LocationEngineResult>>()
        val result = mockk<LocationEngineResult>()
        val callbackCaptor = slot<LocationEngineCallback<LocationEngineResult>>()

        wrapper.getLastLocation(callback)

        verify(exactly = 1) {
            originalEngine.getLastLocation(capture(callbackCaptor))
        }

        wrapper.getLastLocation(secondCallback)
        wrapper.cancelLastLocationTask(secondCallback)

        callbackCaptor.captured.onSuccess(result)

        verify(exactly = 1) { callback.onSuccess(result) }
    }

    @Test
    fun getLastLocationFailureAfterCancellingAnotherTask() {
        val secondCallback = mockk<LocationEngineCallback<LocationEngineResult>>()
        val exception = IllegalArgumentException()
        val callbackCaptor = slot<LocationEngineCallback<LocationEngineResult>>()

        wrapper.getLastLocation(callback)

        verify(exactly = 1) {
            originalEngine.getLastLocation(capture(callbackCaptor))
        }

        wrapper.getLastLocation(secondCallback)
        wrapper.cancelLastLocationTask(secondCallback)

        callbackCaptor.captured.onFailure(exception)

        verify(exactly = 1) { callback.onFailure(exception) }
    }
}
