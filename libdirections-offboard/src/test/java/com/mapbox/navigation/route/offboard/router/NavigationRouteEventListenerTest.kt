package com.mapbox.navigation.route.offboard.router

import com.mapbox.navigation.utils.time.ElapsedTime
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import org.junit.Test

class NavigationRouteEventListenerTest {

    @Test
    fun callStart_timeStartIsCalled() {
        val time = mockk<ElapsedTime>(relaxed = true)
        val call = mockk<Call>(relaxed = true)
        val listener = NavigationRouteEventListener(time)

        listener.callStart(call)

        verify { time.start() }
    }

    @Test
    fun callEnd_timeEndIsCalled() {
        val time = mockk<ElapsedTime>(relaxed = true)
        val call = mockk<Call>(relaxed = true)
        val listener = NavigationRouteEventListener(time)

        listener.callEnd(call)

        verify { time.end() }
    }
}
