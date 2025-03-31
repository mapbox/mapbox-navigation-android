package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.geojson.Point

internal sealed class ViewportProperty<T>(var override: T?, var fallback: T) {

    fun get() = override ?: fallback

    class CenterProperty(override: Point?, fallback: Point) : ViewportProperty<Point>(
        override,
        fallback,
    )

    class ZoomProperty(override: Double?, fallback: Double) : ViewportProperty<Double>(
        override,
        fallback,
    )

    class BearingProperty(override: Double?, fallback: Double) : ViewportProperty<Double>(
        override,
        fallback,
    )

    class PitchProperty(override: Double?, fallback: Double) : ViewportProperty<Double>(
        override,
        fallback,
    )

    class BooleanProperty(override: Boolean?, fallback: Boolean) : ViewportProperty<Boolean>(
        override,
        fallback,
    )
}
