package com.mapbox.navigation.core.replay.route

fun List<Double>.removeAccelerationAndBrakingSpeedUpdates(): List<Double> {
    val accelerationEndsOn = findEndOfInitialAcceleration()
    val brakingStartsOn = findBeginningOfBraking()
    return take(brakingStartsOn)
        .drop(accelerationEndsOn)
}

private fun List<Double>.findEndOfInitialAcceleration(): Int {
    var accelerationEndsOn = 0
    for (i in 1 until this.size) {
        if (this[i] <= this[i - 1]) {
            accelerationEndsOn = i
            break
        }
    }
    return accelerationEndsOn
}

private fun List<Double>.findBeginningOfBraking(): Int {
    var brakingStartsOn = 0
    for (i in this.size - 1 downTo 2) {
        if (this[i] >= this[i - 1]) {
            brakingStartsOn = i
            break
        }
    }
    return brakingStartsOn
}
