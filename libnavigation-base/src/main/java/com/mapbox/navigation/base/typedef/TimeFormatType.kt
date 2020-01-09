package com.mapbox.navigation.base.typedef

import androidx.annotation.IntDef

const val NONE_SPECIFIED = -1
const val TWELVE_HOURS = 0
const val TWENTY_FOUR_HOURS = 1

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    NONE_SPECIFIED,
    TWELVE_HOURS,
    TWENTY_FOUR_HOURS
)
annotation class TimeFormatType
