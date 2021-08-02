package com.mapbox.navigation.examples.core

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.kmm.geojson.Point
import com.mapbox.kmm.turf.TurfConstants
import com.mapbox.kmm.turf.TurfMeasurement

class MapboxTurfKmmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turf_kmm)

        val bearing: TextView = findViewById(R.id.bearing)
        val origin: Point = Point.fromLngLat(-121.435678, 37.899657)
        val destination: Point = Point.fromLngLat(-121.123678, 37.544657)
        bearing.text = "Bearing:\n${TurfMeasurement.bearing(origin, destination)}"

        val distance: TextView = findViewById(R.id.distance)
        distance.text = "Distance:\n${
            TurfMeasurement.distance(
                origin,
                destination
            )
        }"
    }
}
