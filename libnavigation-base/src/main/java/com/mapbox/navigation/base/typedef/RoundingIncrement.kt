package com.mapbox.navigation.base.typedef

import androidx.annotation.IntDef

/**
 * Rounding increment 5
 *
 * Used at [RoundingIncrement]
 */
const val ROUNDING_INCREMENT_FIVE = 5

/**
 * Rounding increment 10
 *
 * Used at [RoundingIncrement]
 */
const val ROUNDING_INCREMENT_TEN = 10

/**
 * Rounding increment 25
 *
 * Used at [RoundingIncrement]
 */
const val ROUNDING_INCREMENT_TWENTY_FIVE = 25

/**
 * Rounding increment 50
 *
 * Used at [RoundingIncrement]
 */
const val ROUNDING_INCREMENT_FIFTY = 50

/**
 * Rounding increment 100
 *
 * Used at [RoundingIncrement]
 */
const val ROUNDING_INCREMENT_ONE_HUNDRED = 100

/**
 * Defines the increment displayed on the instruction view
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ROUNDING_INCREMENT_FIVE,
    ROUNDING_INCREMENT_TEN,
    ROUNDING_INCREMENT_TWENTY_FIVE,
    ROUNDING_INCREMENT_FIFTY,
    ROUNDING_INCREMENT_ONE_HUNDRED
)
annotation class RoundingIncrement
