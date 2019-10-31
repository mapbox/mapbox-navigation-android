package com.mapbox.navigation.base.typedef

import androidx.annotation.StringDef
import com.mapbox.navigation.base.model.route.RouteConstants

@Retention(AnnotationRetention.SOURCE)
@StringDef(
        RouteConstants.IMPERIAL,
        RouteConstants.METRIC)
annotation class VoiceUnitCriteria
