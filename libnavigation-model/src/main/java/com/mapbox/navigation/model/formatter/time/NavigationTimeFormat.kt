@file:JvmName("NavigationTimeFormatKt")

package com.mapbox.navigation.model.formatter.time

import androidx.annotation.IntDef

@Retention(AnnotationRetention.RUNTIME)
@IntDef(NONE_SPECIFIED, TWELVE_HOURS, TWENTY_FOUR_HOURS)
annotation class TimeFormatType

const val NONE_SPECIFIED = -1
const val TWELVE_HOURS = 0
const val TWENTY_FOUR_HOURS = 1
