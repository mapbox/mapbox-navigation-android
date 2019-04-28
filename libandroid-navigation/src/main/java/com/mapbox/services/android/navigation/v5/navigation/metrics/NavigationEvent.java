package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Build;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.mapbox.android.telemetry.Event;
import com.mapbox.services.android.navigation.BuildConfig;

/**
 * Base Event class for navigation events, contains common properties.
 */
abstract class NavigationEvent extends Event {
  private static final String OPERATING_SYSTEM = "Android - " + Build.VERSION.RELEASE;
  private final String operatingSystem = OPERATING_SYSTEM;
  private final String device = Build.MODEL;
  private final String sdkVersion = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME;
  private final String event;
  private final String created;
  private final String applicationState;
  private final String connectivity;
  private final boolean batteryPluggedIn;
  private final int volumeLevel;
  private final int screenBrightness;
  private final int batteryLevel;
  private String startTimestamp;
  private String sdkIdentifier;
  private String sessionIdentifier;
  private String geometry;
  private String profile;
  private String originalRequestIdentifier;
  private String requestIdentifier;
  private String originalGeometry;
  private String audioType;
  private String locationEngine;
  private String tripIdentifier;
  private double lat;
  private double lng;
  private boolean simulation;
  private int absoluteDistanceToDestination;
  private int percentTimeInPortrait;
  private int percentTimeInForeground;
  private int distanceCompleted;
  private int distanceRemaining;
  private int durationRemaining;
  private int eventVersion;
  private int estimatedDistance;
  private int estimatedDuration;
  private int rerouteCount;
  private int originalEstimatedDistance;
  private int originalEstimatedDuration;
  private int stepCount;
  private int originalStepCount;
  private int legIndex;
  private int legCount;
  private int stepIndex;
  private int voiceIndex;
  private int bannerIndex;
  private int totalStepCount;

  NavigationEvent(@NonNull PhoneState phoneState) {
    this.created = phoneState.getCreated();
    this.volumeLevel = phoneState.getVolumeLevel();
    this.batteryLevel = phoneState.getBatteryLevel();
    this.screenBrightness = phoneState.getScreenBrightness();
    this.batteryPluggedIn = phoneState.isBatteryPluggedIn();
    this.connectivity = phoneState.getConnectivity();
    this.audioType = phoneState.getAudioType();
    this.applicationState = phoneState.getApplicationState();
    this.event = getEventName();
  }

  abstract String getEventName();

  String getEvent() {
    return event;
  }

  String getOperatingSystem() {
    return operatingSystem;
  }

  String getDevice() {
    return device;
  }

  String getCreated() {
    return created;
  }

  String getApplicationState() {
    return applicationState;
  }

  String getConnectivity() {
    return connectivity;
  }

  boolean isBatteryPluggedIn() {
    return batteryPluggedIn;
  }

  int getVolumeLevel() {
    return volumeLevel;
  }

  int getScreenBrightness() {
    return screenBrightness;
  }

  int getBatteryLevel() {
    return batteryLevel;
  }

  String getStartTimestamp() {
    return startTimestamp;
  }

  void setStartTimestamp(String startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  String getSdkIdentifier() {
    return sdkIdentifier;
  }

  void setSdkIdentifier(String sdkIdentifier) {
    this.sdkIdentifier = sdkIdentifier;
  }

  String getSdkVersion() {
    return sdkVersion;
  }

  String getSessionIdentifier() {
    return sessionIdentifier;
  }

  void setSessionIdentifier(String sessionIdentifier) {
    this.sessionIdentifier = sessionIdentifier;
  }

  String getGeometry() {
    return geometry;
  }

  void setGeometry(String geometry) {
    this.geometry = geometry;
  }

  String getProfile() {
    return profile;
  }

  void setProfile(String profile) {
    this.profile = profile;
  }

  String getOriginalRequestIdentifier() {
    return originalRequestIdentifier;
  }

  void setOriginalRequestIdentifier(String originalRequestIdentifier) {
    this.originalRequestIdentifier = originalRequestIdentifier;
  }

  String getRequestIdentifier() {
    return requestIdentifier;
  }

  void setRequestIdentifier(String requestIdentifier) {
    this.requestIdentifier = requestIdentifier;
  }

  String getOriginalGeometry() {
    return originalGeometry;
  }

  void setOriginalGeometry(String originalGeometry) {
    this.originalGeometry = originalGeometry;
  }

  String getAudioType() {
    return audioType;
  }

  void setAudioType(String audioType) {
    this.audioType = audioType;
  }

  String getLocationEngine() {
    return locationEngine;
  }

  void setLocationEngine(String locationEngine) {
    this.locationEngine = locationEngine;
  }

  String getTripIdentifier() {
    return tripIdentifier;
  }

  void setTripIdentifier(String tripIdentifier) {
    this.tripIdentifier = tripIdentifier;
  }

  double getLat() {
    return lat;
  }

  void setLat(double lat) {
    this.lat = lat;
  }

  double getLng() {
    return lng;
  }

  void setLng(double lng) {
    this.lng = lng;
  }

  boolean isSimulation() {
    return simulation;
  }

  void setSimulation(boolean simulation) {
    this.simulation = simulation;
  }

  int getAbsoluteDistanceToDestination() {
    return absoluteDistanceToDestination;
  }

  void setAbsoluteDistanceToDestination(int absoluteDistanceToDestination) {
    this.absoluteDistanceToDestination = absoluteDistanceToDestination;
  }

  int getPercentTimeInPortrait() {
    return percentTimeInPortrait;
  }

  void setPercentTimeInPortrait(int percentTimeInPortrait) {
    this.percentTimeInPortrait = percentTimeInPortrait;
  }

  int getPercentTimeInForeground() {
    return percentTimeInForeground;
  }

  void setPercentTimeInForeground(int percentTimeInForeground) {
    this.percentTimeInForeground = percentTimeInForeground;
  }

  int getDistanceCompleted() {
    return distanceCompleted;
  }

  void setDistanceCompleted(int distanceCompleted) {
    this.distanceCompleted = distanceCompleted;
  }

  int getDistanceRemaining() {
    return distanceRemaining;
  }

  void setDistanceRemaining(int distanceRemaining) {
    this.distanceRemaining = distanceRemaining;
  }

  int getDurationRemaining() {
    return durationRemaining;
  }

  void setDurationRemaining(int durationRemaining) {
    this.durationRemaining = durationRemaining;
  }

  int getEventVersion() {
    return eventVersion;
  }

  void setEventVersion(int eventVersion) {
    this.eventVersion = eventVersion;
  }

  int getEstimatedDistance() {
    return estimatedDistance;
  }

  void setEstimatedDistance(int estimatedDistance) {
    this.estimatedDistance = estimatedDistance;
  }

  int getEstimatedDuration() {
    return estimatedDuration;
  }

  void setEstimatedDuration(int estimatedDuration) {
    this.estimatedDuration = estimatedDuration;
  }

  int getRerouteCount() {
    return rerouteCount;
  }

  void setRerouteCount(int rerouteCount) {
    this.rerouteCount = rerouteCount;
  }

  int getOriginalEstimatedDistance() {
    return originalEstimatedDistance;
  }

  void setOriginalEstimatedDistance(int originalEstimatedDistance) {
    this.originalEstimatedDistance = originalEstimatedDistance;
  }

  int getOriginalEstimatedDuration() {
    return originalEstimatedDuration;
  }

  void setOriginalEstimatedDuration(int originalEstimatedDuration) {
    this.originalEstimatedDuration = originalEstimatedDuration;
  }

  int getStepCount() {
    return stepCount;
  }

  void setStepCount(int stepCount) {
    this.stepCount = stepCount;
  }

  int getOriginalStepCount() {
    return originalStepCount;
  }

  void setOriginalStepCount(int originalStepCount) {
    this.originalStepCount = originalStepCount;
  }

  int getLegIndex() {
    return legIndex;
  }

  void setLegIndex(int legIndex) {
    this.legIndex = legIndex;
  }

  int getLegCount() {
    return legCount;
  }

  void setLegCount(int legCount) {
    this.legCount = legCount;
  }

  int getStepIndex() {
    return stepIndex;
  }

  void setStepIndex(int stepIndex) {
    this.stepIndex = stepIndex;
  }

  int getVoiceIndex() {
    return voiceIndex;
  }

  void setVoiceIndex(int voiceIndex) {
    this.voiceIndex = voiceIndex;
  }

  int getBannerIndex() {
    return bannerIndex;
  }

  void setBannerIndex(int bannerIndex) {
    this.bannerIndex = bannerIndex;
  }

  int getTotalStepCount() {
    return totalStepCount;
  }

  void setTotalStepCount(int totalStepCount) {
    this.totalStepCount = totalStepCount;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
  }
}