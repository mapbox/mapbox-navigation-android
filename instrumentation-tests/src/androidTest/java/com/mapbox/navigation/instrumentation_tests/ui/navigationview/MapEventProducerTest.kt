package com.mapbox.navigation.instrumentation_tests.ui.navigationview

import android.location.Location
import android.os.SystemClock
import android.view.MotionEvent
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.dropin.component.map.MapEventProducer
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.testing.ui.BaseTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class MapEventProducerTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java
) {

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 37.7576948
            longitude = -122.4727051
        }
    }

    @Test
    fun mapviewLongClicks() = runBlocking {
        val delegate = MapEventProducer(activity.binding.mapView)
        val def = async {
            delegate.mapLongClicks.first()
        }
        delay(100)
        val me = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 500,
            MotionEvent.ACTION_DOWN,
            15f,
            10f,
            0
        )
        activity.binding.mapView.gestures.getGesturesManager().onTouchEvent(me)

        val result = def.await()

        assertNotNull(result)
    }

    @Test
    fun mapviewClicks() = runBlocking {
        val delegate = MapEventProducer(activity.binding.mapView)
        val def = async {
            delegate.mapClicks.first()
        }
        delay(100)
        val motionEventDown = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            15f,
            10f,
            0
        )
        val motionEventUp = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP,
            15f,
            10f,
            0
        )
        activity.binding.mapView.gestures.getGesturesManager().onTouchEvent(motionEventDown)
        activity.binding.mapView.gestures.getGesturesManager().onTouchEvent(motionEventUp)

        val result = def.await()

        assertNotNull(result)
    }

    @Test
    fun mapMoveTest() = runBlocking {
        val delegate = MapEventProducer(activity.binding.mapView)
        val def = async {
            delegate.mapMovements.first()
        }
        delay(100)

        val motionEventDown = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 1,
            MotionEvent.ACTION_DOWN,
            200f,
            200f,
            0
        )

        val motionEventMove = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 50,
            MotionEvent.ACTION_MOVE,
            200f,
            200f,
            0
        )

        val motionEventUp = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 1,
            MotionEvent.ACTION_UP,
            200f,
            200f,
            0
        )
        activity.binding.mapView.gestures.getGesturesManager().onTouchEvent(motionEventDown)
        activity.binding.mapView.gestures.getGesturesManager().onTouchEvent(motionEventMove)
        activity.binding.mapView.gestures.getGesturesManager().onTouchEvent(motionEventUp)

        val result = def.await()

        assertNotNull(result)
    }
}
