package com.mapbox.navigation.mapgpt.core

open class MiddlewareProvider(val key: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as MiddlewareProvider

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
