package com.mapbox.navigation.examples.util

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.test.espresso.IdlingResource
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.navigation.examples.R
import java.util.concurrent.atomic.AtomicBoolean
import junit.framework.TestCase.assertNotNull

class OnMapReadyIdlingResource
constructor(
    activity: Activity
) : IdlingResource, OnMapReadyCallback {

    private val isIdling = AtomicBoolean(false)

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    init {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val mapView: MapView = activity.findViewById(R.id.mapView)
            mapView.getMapAsync(this@OnMapReadyIdlingResource)
        }
    }

    override fun getName(): String = javaClass.simpleName

    override fun isIdleNow(): Boolean = isIdling.get()

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        assertNotNull("MapboxMap should not be null", mapboxMap)
        mapboxMap.getStyle { _ ->
            isIdling.set(true)
            resourceCallback?.onTransitionToIdle()
        }
    }
}
