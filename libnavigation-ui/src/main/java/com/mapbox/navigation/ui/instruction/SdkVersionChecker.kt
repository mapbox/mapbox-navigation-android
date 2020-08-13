package com.mapbox.navigation.ui.instruction

/**
 * The file is used to verify if the current Android SDK Version against
 * another Android SDK Version
 *
 * @param currentSdkVersion The current version of Android SDK
 */
internal class SdkVersionChecker(private val currentSdkVersion: Int) {

    /**
     * Checks if the current SDK version is equal to or greater than the sdkCode
     *
     * @param sdkCode android SDK Code
     * @return true if the sdkCode is equal or greater, false if not
     */
    fun isEqualOrGreaterThan(sdkCode: Int): Boolean =
        currentSdkVersion >= sdkCode
}
