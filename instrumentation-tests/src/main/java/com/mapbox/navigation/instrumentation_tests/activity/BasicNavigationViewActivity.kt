package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.navigation.instrumentation_tests.R
import kotlinx.android.synthetic.main.activity_basic_navigation_view.*

class BasicNavigationViewActivity : AppCompatActivity() {

    lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_view)
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(
            "asset://map_style_blank.json",
            onMapLoadErrorListener = object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                    Log.e("onMapLoadError", mapViewLoadError.name + ": " + msg)
                }
            }
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }
}
