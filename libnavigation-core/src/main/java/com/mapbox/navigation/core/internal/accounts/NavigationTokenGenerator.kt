package com.mapbox.navigation.core.internal.accounts

internal interface NavigationTokenGenerator {
    fun getSKUToken(): String
}
