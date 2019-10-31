package com.mapbox.navigation.base.typedef

import androidx.annotation.IntDef
import com.mapbox.navigation.base.model.route.RouteConstants

@Retention(AnnotationRetention.RUNTIME)
@IntDef(RouteConstants.NONE_SPECIFIED,
        RouteConstants.TWELVE_HOURS,
        RouteConstants.TWENTY_FOUR_HOURS)
annotation class TimeFormatType
