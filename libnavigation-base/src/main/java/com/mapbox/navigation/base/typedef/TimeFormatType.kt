package com.mapbox.navigation.base.typedef

import androidx.annotation.IntDef

/**
 * Time format: defined by the device's settings
 *
 * Used at [TimeFormatType]
 */
const val NONE_SPECIFIED = -1

/**
 * Time format: 12-hour (11.00PM)
 *
 * Used at [TimeFormatType]
 */
const val TWELVE_HOURS = 0

/**
 * Time format: 24-hour (23.00)
 *
 * Used at [TimeFormatType]
 */
const val TWENTY_FOUR_HOURS = 1

/**
 * Defines time format for calculation remaining trip time.
 *
 * When [TWELVE_HOURS] is selected -> 11.00PM
 *
 * When [TWENTY_FOUR_HOURS] is selected -> 23.00
 *
 * When [NONE_SPECIFIED] is selected -> Depends on user's device settings
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    NONE_SPECIFIED,
    TWELVE_HOURS,
    TWENTY_FOUR_HOURS
)
annotation class TimeFormatType
