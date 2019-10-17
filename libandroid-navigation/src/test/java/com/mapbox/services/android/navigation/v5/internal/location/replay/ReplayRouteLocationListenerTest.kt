package com.mapbox.services.android.navigation.v5.internal.location.replay

import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ReplayRouteLocationListenerTest {

    @Test
    fun onLocationReplay_lastMockedLocationRemoved() {
        val engine = mockk<ReplayRouteLocationEngine>(relaxed = true)
        val callback = mockk<LocationEngineCallback<LocationEngineResult>>(relaxed = true)
        val listener =
            ReplayRouteLocationListener(
                engine,
                callback
            )

        listener.onLocationReplay(mockk())

        verify { engine.removeLastMockedLocation() }
    }

    @Test
    fun onLocationReplay_updateLastLocation() {
        val engine = mockk<ReplayRouteLocationEngine>(relaxed = true)
        val callback = mockk<LocationEngineCallback<LocationEngineResult>>(relaxed = true)
        val location = mockk<Location>()
        val listener =
            ReplayRouteLocationListener(
                engine,
                callback
            )

        listener.onLocationReplay(location)

        verify { engine.updateLastLocation(location) }
    }
}
