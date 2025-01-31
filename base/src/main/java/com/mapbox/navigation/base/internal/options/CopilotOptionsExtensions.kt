package com.mapbox.navigation.base.internal.options

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.CopilotOptions

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun CopilotOptions.Builder.setOwner(ownerName: String): CopilotOptions.Builder =
    apply { ownerName(ownerName) }

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun CopilotOptions.getOwner() = this.ownerName
