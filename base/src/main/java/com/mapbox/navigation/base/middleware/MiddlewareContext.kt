package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Represents a middleware context that provides shared components and dependencies
 * required by various middleware components.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface MiddlewareContext {

    @ExperimentalPreviewMapboxNavigationAPI
    object Empty : MiddlewareContext {
        override fun toString(): String = "MiddlewareContext.Empty"
    }
}
