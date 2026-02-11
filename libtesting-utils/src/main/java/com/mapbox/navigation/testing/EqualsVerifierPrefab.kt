package com.mapbox.navigation.testing

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.geojson.Point
import nl.jqno.equalsverifier.api.SingleTypeEqualsVerifierApi

fun <T> SingleTypeEqualsVerifierApi<T>.withPrefabTestPoint(
    red: Point = Point.fromLngLat(1.0, 2.0),
    blue: Point = Point.fromLngLat(3.0, 4.0)
): SingleTypeEqualsVerifierApi<T> {
    return withPrefabValues(Point::class.java, red, blue)
}

fun ToStringVerifier.withPrefabTestPoint(
    point: Point = Point.fromLngLat(1.0, 2.0),
): ToStringVerifier {
    return withPrefabValue(Point::class.java, point)
}
