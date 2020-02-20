package com.mapbox.navigation.ui.legacy

class SdkVersionChecker(private val currentSdkVersion: Int) {

    fun isGreaterThan(sdkCode: Int): Boolean =
        currentSdkVersion > sdkCode

    fun isEqualOrGreaterThan(sdkCode: Int): Boolean =
        currentSdkVersion >= sdkCode
}
