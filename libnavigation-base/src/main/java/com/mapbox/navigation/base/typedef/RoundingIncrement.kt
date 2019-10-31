package com.mapbox.navigation.base.typedef

import androidx.annotation.IntDef
import com.mapbox.navigation.base.model.route.RouteConstants

@Retention(AnnotationRetention.SOURCE)
@IntDef(
        RouteConstants.ROUNDING_INCREMENT_FIVE,
        RouteConstants.ROUNDING_INCREMENT_TEN,
        RouteConstants.ROUNDING_INCREMENT_TWENTY_FIVE,
        RouteConstants.ROUNDING_INCREMENT_FIFTY,
        RouteConstants.ROUNDING_INCREMENT_ONE_HUNDRED
)
annotation class RoundingIncrement
