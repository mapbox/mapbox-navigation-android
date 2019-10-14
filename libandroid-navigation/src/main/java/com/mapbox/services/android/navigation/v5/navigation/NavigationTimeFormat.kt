@file:JvmName("NavigationTimeFormatKt")

package com.mapbox.services.android.navigation.v5.navigation

import androidx.annotation.IntDef

@Retention(AnnotationRetention.RUNTIME)
@IntDef(NONE_SPECIFIED, TWELVE_HOURS, TWENTY_FOUR_HOURS)
annotation class TimeFormatType

const val NONE_SPECIFIED = -1
const val TWELVE_HOURS = 0
const val TWENTY_FOUR_HOURS = 1
