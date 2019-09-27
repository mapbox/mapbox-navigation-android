package com.mapbox.services.android.navigation.v5.milestone

/**
 * Extracted operation methods are found in this class and are fundamental to how Triggers work.
 *
 * @since 0.4.0
 */
private object Operation {

    fun greaterThan(valueOne: Array<Number>, valueTwo: Number): Boolean {
        return if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() > valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() <= valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() > valueTwo.toDouble()
    }

    fun lessThan(valueOne: Array<Number>, valueTwo: Number): Boolean {
        return if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() < valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() >= valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() < valueTwo.toDouble()
    }

    fun notEqual(valueOne: Array<Number>, valueTwo: Number): Boolean {
        return if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0] != valueOne[1]
            } else {
                valueOne[0] == valueOne[1]
            }
        } else valueOne[0] != valueTwo
    }

    fun equal(valueOne: Array<Number>, valueTwo: Number): Boolean {
        return if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0] == valueOne[1]
            } else {
                valueOne[0] != valueOne[1]
            }
        } else valueOne[0] == valueTwo
    }

    fun greaterThanEqual(valueOne: Array<Number>, valueTwo: Number): Boolean {
        return if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() >= valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() < valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() >= valueTwo.toDouble()
    }

    fun lessThanEqual(valueOne: Array<Number>, valueTwo: Number): Boolean {
        return if (valueOne.size > 1) {
            if (valueTwo == TriggerProperty.TRUE) {
                valueOne[0].toDouble() <= valueOne[1].toDouble()
            } else {
                valueOne[0].toDouble() > valueOne[1].toDouble()
            }
        } else valueOne[0].toDouble() <= valueTwo.toDouble()
    }

}// Private constructor to prevent initialization of class.
