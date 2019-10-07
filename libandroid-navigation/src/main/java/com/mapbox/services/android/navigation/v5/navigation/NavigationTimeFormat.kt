package com.mapbox.services.android.navigation.v5.navigation

object NavigationTimeFormat {
//    @Retention(RetentionPolicy.SOURCE)
//    @IntDef(NONE_SPECIFIED, TWELVE_HOURS, TWENTY_FOUR_HOURS)
    annotation class Type
    @JvmStatic
    val NONE_SPECIFIED = -1
    @JvmStatic
    val TWELVE_HOURS = 0
    @JvmStatic
    val TWENTY_FOUR_HOURS = 1
}
