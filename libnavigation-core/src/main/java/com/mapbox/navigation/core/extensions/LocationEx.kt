package com.mapbox.navigation.core.extensions

import android.location.Location
import com.mapbox.navigator.FixLocation

// internal fun FixLocation.toLocation(): Location = Location(this.provider).also {
//     it.latitude = this.coordinate.latitude()
//     it.longitude = this.coordinate.longitude()
//     it.time = this.time.time
//     it.speed = this.speed ?: 0f
//     it.bearing = this.bearing ?: 0f
//     it.altitude = this.altitude?.toDouble() ?: 0.0
//     it.accuracy = this.accuracyHorizontal ?: 0f
// }