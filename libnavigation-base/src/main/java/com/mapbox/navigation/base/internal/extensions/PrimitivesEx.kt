package com.mapbox.navigation.base.internal.extensions

fun Double?.equals(other: Double?): Boolean {
    return compareValues(this, other) == 0
}

fun Double?.notEquals(other: Double?): Boolean = this.equals(other).not()


fun Float?.equals(other: Float?): Boolean {
    return compareValues(this, other) == 0
}

fun Float?.notEquals(other: Float?): Boolean = this.equals(other).not()
