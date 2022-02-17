package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.base.internal.route.InternalRouter
import com.mapbox.navigation.base.route.Router

/**
 * Check if [Router] interface impl in Nav SDK
 */
internal fun Router.isInternalImplementation(): Boolean = this is InternalRouter
