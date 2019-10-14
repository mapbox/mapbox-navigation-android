package com.mapbox.services.android.navigation.v5.milestone

object Operation {

    @JvmStatic
    fun greaterThan(valueOne: Array<Number>, valueTwo: Number): Boolean =
        if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() > valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() <= valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() > valueTwo.toDouble()

    @JvmStatic
    fun lessThan(valueOne: Array<Number>, valueTwo: Number): Boolean =
        if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() < valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() >= valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() < valueTwo.toDouble()

    @JvmStatic
    fun notEqual(valueOne: Array<Number>, valueTwo: Number): Boolean =
        if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0] != valueOne[1]
            } else {
                valueOne[0] == valueOne[1]
            }
        } else valueOne[0] != valueTwo

    @JvmStatic
    fun equal(valueOne: Array<Number>, valueTwo: Number): Boolean =
        if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0] == valueOne[1]
            } else {
                valueOne[0] != valueOne[1]
            }
        } else valueOne[0] == valueTwo

    @JvmStatic
    fun greaterThanEqual(valueOne: Array<Number>, valueTwo: Number): Boolean =
        if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() >= valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() < valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() >= valueTwo.toDouble()

    @JvmStatic
    fun lessThanEqual(valueOne: Array<Number>, valueTwo: Number): Boolean =
        if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() <= valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() > valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() <= valueTwo.toDouble()
}
