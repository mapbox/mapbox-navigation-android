package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.content.Context;
import android.os.Build;

import com.mapbox.android.telemetry.TelemetryUtils;

import java.util.Date;

public class NavigationMetadata {
  private static final String OPERATING_SYSTEM = "Android - " + Build.VERSION.RELEASE;
  private int absoluteDistanceToDestination;
  private Integer percentTimeInPortrait = null;
  private Integer percentTimeInForeground = null;
  private String startTimestamp;
  private int distanceCompleted;
  private int distanceRemaining;
  private int durationRemaining;
  private String operatingSystem;
  private int eventVersion;
  private String sdkIdentifier;
  private String sdkVersion;
  private String sessionIdentifier;
  private double lat;
  private double lng;
  private String geometry;
  private String created;
  private String profile;
  private Integer estimatedDistance = null;
  private Integer estimatedDuration = null;
  private Integer rerouteCount = null;
  private boolean simulation;
  private String originalRequestIdentifier = null;
  private String requestIdentifier = null;
  private String originalGeometry = null;
  private Integer originalEstimatedDistance = null;
  private Integer originalEstimatedDuration = null;
  private String audioType;
  private int stepCount;
  private Integer originalStepCount = null;
  private String device;
  private String locationEngine;
  private int volumeLevel;
  private int screenBrightness;
  private String applicationState;
  private Boolean batteryPluggedIn;
  private int batteryLevel;
  private String connectivity;
  private String tripIdentifier;
  private int legIndex;
  private int legCount;
  private int stepIndex;
  private Integer voiceIndex = null;
  private Integer bannerIndex = null;
  private int totalStepCount;

  public NavigationMetadata(Date startTimestamp, int distanceCompleted, int distanceRemaining, int durationRemaining,
                            String sdkIdentifier, String sdkVersion, int eventVersion, String sessionIdentifier,
                            double lat, double lng, String geometry, String profile, boolean isSimulation,
                            String locationEngine, int absoluteDistanceToDestination, String tripIdentifier,
                            int legIndex, int legCount, int stepIndex, int stepCount, int totalStepCount) {
    this.startTimestamp = TelemetryUtils.generateCreateDateFormatted(startTimestamp);
    this.distanceCompleted = distanceCompleted;
    this.distanceRemaining = distanceRemaining;
    this.durationRemaining = durationRemaining;
    this.operatingSystem = OPERATING_SYSTEM;
    this.sdkIdentifier = sdkIdentifier;
    this.sdkVersion = sdkVersion;
    this.eventVersion = eventVersion;
    this.sessionIdentifier = sessionIdentifier;
    this.lat = lat;
    this.lng = lng;
    this.geometry = geometry;
    this.created = TelemetryUtils.obtainCurrentDate();
    this.profile = profile;
    this.simulation = isSimulation;
    this.device = Build.MODEL;
    this.locationEngine = locationEngine;
    this.absoluteDistanceToDestination = absoluteDistanceToDestination;
    this.volumeLevel = 0;
    this.batteryLevel = 0;
    this.screenBrightness = 0;
    this.batteryPluggedIn = false;
    this.connectivity = "";
    this.audioType = "";
    this.applicationState = "";
    this.tripIdentifier = tripIdentifier;
    this.legIndex = legIndex;
    this.legCount = legCount;
    this.stepIndex = stepIndex;
    this.stepCount = stepCount;
    this.totalStepCount = totalStepCount;
  }

  NavigationMetadata setDeviceInfo(Context context) {
    this.volumeLevel = NavigationUtils.obtainVolumeLevel(context);
    this.batteryLevel = TelemetryUtils.obtainBatteryLevel(context);
    this.screenBrightness = NavigationUtils.obtainScreenBrightness(context);
    this.batteryPluggedIn = TelemetryUtils.isPluggedIn(context);
    this.connectivity = TelemetryUtils.obtainCellularNetworkType(context);
    this.audioType = NavigationUtils.obtainAudioType(context);
    this.applicationState = TelemetryUtils.obtainApplicationState(context);
    return this;
  }

  public void setCreated(Date created) {
    this.created = TelemetryUtils.generateCreateDateFormatted(created);
  }

  String getCreated() {
    return created;
  }

  String getStartTimestamp() {
    return startTimestamp;
  }

  Integer getDistanceCompleted() {
    return distanceCompleted;
  }

  Integer getDistanceRemaining() {
    return distanceRemaining;
  }

  Integer getDurationRemaining() {
    return durationRemaining;
  }

  String getOperatingSystem() {
    return operatingSystem;
  }

  int getEventVersion() {
    return eventVersion;
  }

  String getSdKIdentifier() {
    return sdkIdentifier;
  }

  String getSdkVersion() {
    return sdkVersion;
  }

  String getSessionIdentifier() {
    return sessionIdentifier;
  }

  double getLat() {
    return lat;
  }

  double getLng() {
    return lng;
  }

  String getGeometry() {
    return geometry;
  }

  String getProfile() {
    return profile;
  }

  Integer getEstimatedDistance() {
    return estimatedDistance;
  }

  public void setEstimatedDistance(Integer estimatedDistance) {
    this.estimatedDistance = estimatedDistance;
  }

  Integer getEstimatedDuration() {
    return estimatedDuration;
  }

  public void setEstimatedDuration(Integer estimatedDuration) {
    this.estimatedDuration = estimatedDuration;
  }

  Integer getRerouteCount() {
    return rerouteCount;
  }

  public void setRerouteCount(Integer rerouteCount) {
    this.rerouteCount = rerouteCount;
  }

  boolean isSimulation() {
    return simulation;
  }

  String getOriginalRequestIdentifier() {
    return originalRequestIdentifier;
  }

  public void setOriginalRequestIdentifier(String originalRequestIdentifier) {
    this.originalRequestIdentifier = originalRequestIdentifier;
  }

  String getRequestIdentifier() {
    return requestIdentifier;
  }

  public void setRequestIdentifier(String requestIdentifier) {
    this.requestIdentifier = requestIdentifier;
  }

  String getOriginalGeometry() {
    return originalGeometry;
  }

  public void setOriginalGeometry(String originalGeometry) {
    this.originalGeometry = originalGeometry;
  }

  Integer getOriginalEstimatedDistance() {
    return originalEstimatedDistance;
  }

  public void setOriginalEstimatedDistance(Integer originalEstimatedDistance) {
    this.originalEstimatedDistance = originalEstimatedDistance;
  }

  Integer getOriginalEstimatedDuration() {
    return originalEstimatedDuration;
  }

  public void setOriginalEstimatedDuration(Integer originalEstimatedDuration) {
    this.originalEstimatedDuration = originalEstimatedDuration;
  }

  String getAudioType() {
    return audioType;
  }

  Integer getOriginalStepCount() {
    return originalStepCount;
  }

  public void setOriginalStepCount(Integer originalStepCount) {
    this.originalStepCount = originalStepCount;
  }

  String getDevice() {
    return device;
  }

  String getLocationEngine() {
    return locationEngine;
  }

  Integer getVolumeLevel() {
    return volumeLevel;
  }

  Integer getScreenBrightness() {
    return screenBrightness;
  }

  String getApplicationState() {
    return applicationState;
  }

  Boolean isBatteryPluggedIn() {
    return batteryPluggedIn;
  }

  //for testing
  void setBatteryLevel(Integer batteryLevel) {
    this.batteryLevel = batteryLevel;
  }

  Integer getBatteryLevel() {
    return batteryLevel;
  }

  String getConnectivity() {
    return connectivity;
  }

  int getAbsoluteDistanceToDestination() {
    return absoluteDistanceToDestination;
  }

  Integer getPercentTimeInPortrait() {
    return percentTimeInPortrait;
  }

  public void setPercentTimeInPortrait(Integer percentTimeInPortrait) {
    this.percentTimeInPortrait = percentTimeInPortrait;
  }

  Integer getPercentTimeInForeground() {
    return percentTimeInForeground;
  }

  public void setPercentTimeInForeground(Integer percentTimeInForeground) {
    this.percentTimeInForeground = percentTimeInForeground;
  }

  String getTripIdentifier() {
    return tripIdentifier;
  }

  Integer getLegIndex() {
    return legIndex;
  }

  Integer getLegCount() {
    return legCount;
  }

  Integer getStepIndex() {
    return stepIndex;
  }

  Integer getStepCount() {
    return stepCount;
  }

  Integer getVoiceIndex() {
    return voiceIndex;
  }

  void setVoiceIndex(int voiceIndex) {
    this.voiceIndex = voiceIndex;
  }

  Integer getBannerIndex() {
    return bannerIndex;
  }

  public void setBannerIndex(int bannerIndex) {
    this.bannerIndex = bannerIndex;
  }

  Integer getTotalStepCount() {
    return totalStepCount;
  }
}