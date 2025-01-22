@file:JvmName("LocationEx")

package com.mapbox.navigation.core.navigator

import com.mapbox.bindgen.Value
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationExtraKeys
import com.mapbox.geojson.Point
import com.mapbox.navigator.FixLocation
import java.util.Date
import kotlin.collections.set

internal typealias FixLocationExtras = HashMap<String, Value>

internal fun FixLocation.toLocation(): Location = Location.Builder()
    .latitude(coordinate.latitude())
    .longitude(coordinate.longitude())
    .source(provider)
    .timestamp(time.time)
    .monotonicTimestamp(monotonicTimestampNanoseconds)
    .speed(speed?.toDouble())
    .bearing(bearing?.toDouble())
    .altitude(altitude?.toDouble())
    .horizontalAccuracy(accuracyHorizontal?.toDouble())
    .bearingAccuracy(bearingAccuracy?.toDouble())
    .speedAccuracy(speedAccuracy?.toDouble())
    .verticalAccuracy(verticalAccuracy?.toDouble())
    .extra(Value.valueOf(HashMap(extras).also { it[LocationExtraKeys.IS_MOCK] = Value(isMock) }))
    .build()

internal fun Location.toFixLocation(): FixLocation {
    val extras = HashMap(extra?.contents as? HashMap<String, Value>? ?: emptyMap())
    val isMock = (extras[LocationExtraKeys.IS_MOCK]?.contents as? Boolean?) == true
    extras.remove(LocationExtraKeys.IS_MOCK)
    return FixLocation(
        Point.fromLngLat(longitude, latitude),
        monotonicTimestamp ?: 0,
        Date(timestamp),
        speed?.toFloat(),
        bearing?.toFloat(),
        altitude?.toFloat(),
        horizontalAccuracy?.toFloat(),
        source,
        bearingAccuracy?.toFloat(),
        speedAccuracy?.toFloat(),
        verticalAccuracy?.toFloat(),
        extras,
        isMock,
    )
}

internal fun List<FixLocation>.toLocations(): List<Location> = this.map { it.toLocation() }
