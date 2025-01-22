package com.mapbox.navigation.ui.maps.internal.route.callout.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val RouteCalloutData.internalCallouts get() = callouts
