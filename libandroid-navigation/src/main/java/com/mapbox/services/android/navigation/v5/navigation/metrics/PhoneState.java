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

  public String getUserId() {
    return userId;
  }

  public int getVolumeLevel() {
    return volumeLevel;
  }

  public int getBatteryLevel() {
    return batteryLevel;
  }

  public int getScreenBrightness() {
    return screenBrightness;
  }

  public boolean isBatteryPluggedIn() {
    return batteryPluggedIn;
  }

  public String getConnectivity() {
    return connectivity;
  }

  public String getAudioType() {
    return audioType;
  }

  public String getApplicationState() {
    return applicationState;
  }

  public String getCreated() {
    return created;
  }

  public String getFeedbackId() {
    return feedbackId;
  }
}
