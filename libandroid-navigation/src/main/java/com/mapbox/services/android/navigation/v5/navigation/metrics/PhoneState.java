package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.content.Context;

import com.mapbox.android.telemetry.TelemetryUtils;

/**
 * Class that will hold the current states of the device phone.
 */
public class PhoneState {
  private final int volumeLevel;
  private final int batteryLevel;
  private final int screenBrightness;
  private final boolean batteryPluggedIn;
  private final String connectivity;
  private final String audioType;
  private final String applicationState;
  private final String created;
  private final String feedbackId;
  private final String userId;

  public PhoneState(Context context) {
    this.volumeLevel = NavigationUtils.obtainVolumeLevel(context);
    this.batteryLevel = TelemetryUtils.obtainBatteryLevel(context);
    this.screenBrightness = NavigationUtils.obtainScreenBrightness(context);
    this.batteryPluggedIn = TelemetryUtils.isPluggedIn(context);
    this.connectivity = TelemetryUtils.obtainCellularNetworkType(context);
    this.audioType = NavigationUtils.obtainAudioType(context);
    this.applicationState = TelemetryUtils.obtainApplicationState(context);
    this.created = TelemetryUtils.obtainCurrentDate();
    this.feedbackId = TelemetryUtils.obtainUniversalUniqueIdentifier();
    this.userId = TelemetryUtils.retrieveVendorId();
  }

  String getUserId() {
    return userId;
  }

  int getVolumeLevel() {
    return volumeLevel;
  }

  int getBatteryLevel() {
    return batteryLevel;
  }

  int getScreenBrightness() {
    return screenBrightness;
  }

  boolean isBatteryPluggedIn() {
    return batteryPluggedIn;
  }

  String getConnectivity() {
    return connectivity;
  }

  String getAudioType() {
    return audioType;
  }

  String getApplicationState() {
    return applicationState;
  }

  String getCreated() {
    return created;
  }

  String getFeedbackId() {
    return feedbackId;
  }
}
