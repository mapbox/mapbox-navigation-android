package com.mapbox.navigation.testing.utils

import kotlin.math.abs

class ApproximateDouble(
    private val value: Double,
    private val tolerance: Double = 0.000001
) {
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApproximateDouble

        if (abs(value - other.value) > tolerance) return false

        return true
    }

    override fun hashCode() = 0
}
