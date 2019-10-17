package com.mapbox.services.android.navigation.v5.internal.navigation

class SdkVersionChecker(private val currentSdkVersion: Int) {

    fun isGreaterThan(sdkCode: Int): Boolean =
        currentSdkVersion > sdkCode

    fun isEqualOrGreaterThan(sdkCode: Int): Boolean =
        currentSdkVersion >= sdkCode
}
