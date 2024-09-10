package com.mapbox.navigation.core.internal.congestions.model

@JvmInline
internal value class MetersPerSecond(val value: Float) {
    operator fun compareTo(other: MetersPerSecond): Int = this.value.compareTo(other.value)
    operator fun div(divider: Int): MetersPerSecond = MetersPerSecond(this.value / divider)
    operator fun minus(speed: MetersPerSecond): MetersPerSecond =
        MetersPerSecond(this.value - speed.value)

    operator fun times(multiplier: Int): MetersPerSecond =
        MetersPerSecond(this.value * multiplier)

    operator fun times(multiplier: Float): MetersPerSecond =
        MetersPerSecond(this.value * multiplier)

    companion object {
        fun fromKilometersPerHour(value: Float): MetersPerSecond =
            MetersPerSecond(value * 1000f / 3600f)

        fun fromMilesPerHour(value: Float): MetersPerSecond =
            MetersPerSecond(value * 1609.34f / 3600f)
    }
}

internal fun Number.toMetersPerSecond(): MetersPerSecond = MetersPerSecond(this.toFloat())
