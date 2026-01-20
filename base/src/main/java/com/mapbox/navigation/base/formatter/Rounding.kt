package com.mapbox.navigation.base.formatter

import androidx.annotation.IntDef

/**
 * Rounding
 */
object Rounding {

    /**
     * Undefined rounding increment.
     */
    const val INCREMENT_DISTANCE_DEPENDENT = -1

    /**
     * Rounding increment 5
     *
     * Used at [Increment]
     */
    const val INCREMENT_FIVE = 5

    /**
     * Rounding increment 10
     *
     * Used at [Increment]
     */
    const val INCREMENT_TEN = 10

    /**
     * Rounding increment 25
     *
     * Used at [Increment]
     */
    const val INCREMENT_TWENTY_FIVE = 25

    /**
     * Rounding increment 50
     *
     * Used at [Increment]
     */
    const val INCREMENT_FIFTY = 50

    /**
     * Rounding increment 100
     *
     * Used at [Increment]
     */
    const val INCREMENT_ONE_HUNDRED = 100

    /**
     * Defines the increment displayed on the instruction view
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        INCREMENT_DISTANCE_DEPENDENT,
        INCREMENT_FIVE,
        INCREMENT_TEN,
        INCREMENT_TWENTY_FIVE,
        INCREMENT_FIFTY,
        INCREMENT_ONE_HUNDRED,
    )
    annotation class Increment
}
