package com.mapbox.navigation.base.typedef

import androidx.annotation.IntDef

const val NONE_SPECIFIED = -1
const val TWELVE_HOURS = 0
const val TWENTY_FOUR_HOURS = 1

/**
 * Defines time format for calculation remaining trip time.
 * When [TWELVE_HOURS] is selected -> 11.00PM
 * When [TWENTY_FOUR_HOURS] is selected -> 23.00
 * When [NONE_SPECIFIED] is selected -> Depends on user's device settings
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    NONE_SPECIFIED,
    TWELVE_HOURS,
    TWENTY_FOUR_HOURS
)
annotation class TimeFormatType
