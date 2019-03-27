package com.mapbox.services.android.navigation.v5.internal.navigation;

public class SdkVersionChecker {

  private final int currentSdkVersion;

  public SdkVersionChecker(int currentSdkVersion) {
    this.currentSdkVersion = currentSdkVersion;
  }

  public boolean isGreaterThan(int sdkCode) {
    return currentSdkVersion > sdkCode;
  }

  public boolean isEqualOrGreaterThan(int sdkCode) {
    return currentSdkVersion >= sdkCode;
  }
}
