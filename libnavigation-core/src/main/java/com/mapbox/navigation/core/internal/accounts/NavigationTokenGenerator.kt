package com.mapbox.navigation.core.internal.accounts

internal fun interface NavigationTokenGenerator {
    fun getSKUToken(): String
}
