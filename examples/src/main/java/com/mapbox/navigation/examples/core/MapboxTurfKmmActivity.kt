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
        val pt1: Point = Point.fromLngLat(-75.4, 39.4)
        val pt2: Point = Point.fromLngLat(-75.534, 39.123)
        bearing.text = "Bearing: ${TurfMeasurement.bearing(pt1, pt2)}"

        val distance: TextView = findViewById(R.id.distance)
        val whiteHouse: Point = Point.fromLngLat(-77.03601539811076, 38.90003848157448)
        val goldenGateBridge: Point = Point.fromLngLat(-122.47827, 37.82009)
        distance.text = "Distance White House -> Golden Gate Bridge: ${
            TurfMeasurement.distance(
                whiteHouse,
                goldenGateBridge,
                TurfConstants.UNIT_KILOMETERS
            )
        } km"
    }
}
