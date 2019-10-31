package com.mapbox.navigation.base.typedef

import androidx.annotation.StringDef
import com.mapbox.navigation.base.model.route.RouteConstants

@Retention(AnnotationRetention.SOURCE)
@StringDef(
        RouteConstants.PROFILE_DRIVING_TRAFFIC,
        RouteConstants.PROFILE_DRIVING,
        RouteConstants.PROFILE_WALKING,
        RouteConstants.PROFILE_CYCLING)
annotation class ProfileCriteria
