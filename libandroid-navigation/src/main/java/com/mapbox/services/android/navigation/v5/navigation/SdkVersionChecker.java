package com.mapbox.services.android.navigation.v5.navigation;

class SdkVersionChecker {

  private final int currentSdkVersion;

  SdkVersionChecker(int currentSdkVersion) {
    this.currentSdkVersion = currentSdkVersion;
  }

  boolean isGreaterThan(int sdkCode) {
    return currentSdkVersion > sdkCode;
  }
}
