package com.mapbox.navigation.qa_test_app.domain

import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point

typealias LaunchActivityFun = (AppCompatActivity) -> Unit

data class Destination(val name: String, val point: Point)

val testDestinations = listOf(
    Destination("Newmarket: A&B office", Point.fromLngLat(-79.4443, 44.0620)),
    Destination("Toronto: Lume Kitchen and Lounge", Point.fromLngLat(-79.4843, 43.6244))
)
