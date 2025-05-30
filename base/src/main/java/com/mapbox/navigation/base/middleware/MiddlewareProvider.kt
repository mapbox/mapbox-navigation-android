package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Represents a provider of middleware with a unique key.
 *
 * @param key A unique identifier for the middleware provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
open class MiddlewareProvider(val key: String) {

    /**
     * Checks if another object is equal to this provider based on the key.
     *
     * @param other The object to compare.
     * @return `true` if the keys match, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as MiddlewareProvider

        return key == other.key
    }

    /**
     * Computes the hash code for the middleware provider based on its key.
     *
     * @return The hash code of the key.
     */
    override fun hashCode(): Int {
        return key.hashCode()
    }
}
