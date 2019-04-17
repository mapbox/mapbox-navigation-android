package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Build;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.mapbox.android.telemetry.Event;
import com.mapbox.android.telemetry.TelemetryUtils;
import com.mapbox.services.android.navigation.BuildConfig;

/**
 * Base Event class for navigation events, contains common properties.
 */
public abstract class NavigationEvent extends Event {
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
    this.created = TelemetryUtils.obtainCurrentDate();
    this.volumeLevel = phoneState.getVolumeLevel();
    this.batteryLevel = phoneState.getBatteryLevel();
    this.screenBrightness = phoneState.getScreenBrightness();
    this.batteryPluggedIn = phoneState.isBatteryPluggedIn();
    this.connectivity = phoneState.getConnectivity();
    this.audioType = phoneState.getAudioType();
    this.applicationState = phoneState.getApplicationState();
    this.event = getEventName();
  }

  protected abstract String getEventName();

  public String getEvent() {
    return event;
  }

  public String getOperatingSystem() {
    return operatingSystem;
  }

  public String getDevice() {
    return device;
  }

  public String getCreated() {
    return created;
  }

  public String getApplicationState() {
    return applicationState;
  }

  public String getConnectivity() {
    return connectivity;
  }

  public boolean isBatteryPluggedIn() {
    return batteryPluggedIn;
  }

  public int getVolumeLevel() {
    return volumeLevel;
  }

  public int getScreenBrightness() {
    return screenBrightness;
  }

  public int getBatteryLevel() {
    return batteryLevel;
  }

  public String getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(String startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  public String getSdkIdentifier() {
    return sdkIdentifier;
  }

  public void setSdkIdentifier(String sdkIdentifier) {
    this.sdkIdentifier = sdkIdentifier;
  }

  public String getSdkVersion() {
    return sdkVersion;
  }

  public String getSessionIdentifier() {
    return sessionIdentifier;
  }

  public void setSessionIdentifier(String sessionIdentifier) {
    this.sessionIdentifier = sessionIdentifier;
  }

  public String getGeometry() {
    return geometry;
  }

  public void setGeometry(String geometry) {
    this.geometry = geometry;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public String getOriginalRequestIdentifier() {
    return originalRequestIdentifier;
  }

  public void setOriginalRequestIdentifier(String originalRequestIdentifier) {
    this.originalRequestIdentifier = originalRequestIdentifier;
  }

  public String getRequestIdentifier() {
    return requestIdentifier;
  }

  public void setRequestIdentifier(String requestIdentifier) {
    this.requestIdentifier = requestIdentifier;
  }

  public String getOriginalGeometry() {
    return originalGeometry;
  }

  public void setOriginalGeometry(String originalGeometry) {
    this.originalGeometry = originalGeometry;
  }

  public String getAudioType() {
    return audioType;
  }

  public void setAudioType(String audioType) {
    this.audioType = audioType;
  }

  public String getLocationEngine() {
    return locationEngine;
  }

  public void setLocationEngine(String locationEngine) {
    this.locationEngine = locationEngine;
  }

  public String getTripIdentifier() {
    return tripIdentifier;
  }

  public void setTripIdentifier(String tripIdentifier) {
    this.tripIdentifier = tripIdentifier;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLng() {
    return lng;
  }

  public void setLng(double lng) {
    this.lng = lng;
  }

  public boolean isSimulation() {
    return simulation;
  }

  public void setSimulation(boolean simulation) {
    this.simulation = simulation;
  }

  public int getAbsoluteDistanceToDestination() {
    return absoluteDistanceToDestination;
  }

  public void setAbsoluteDistanceToDestination(int absoluteDistanceToDestination) {
    this.absoluteDistanceToDestination = absoluteDistanceToDestination;
  }

  public int getPercentTimeInPortrait() {
    return percentTimeInPortrait;
  }

  public void setPercentTimeInPortrait(int percentTimeInPortrait) {
    this.percentTimeInPortrait = percentTimeInPortrait;
  }

  public int getPercentTimeInForeground() {
    return percentTimeInForeground;
  }

  public void setPercentTimeInForeground(int percentTimeInForeground) {
    this.percentTimeInForeground = percentTimeInForeground;
  }

  public int getDistanceCompleted() {
    return distanceCompleted;
  }

  public void setDistanceCompleted(int distanceCompleted) {
    this.distanceCompleted = distanceCompleted;
  }

  public int getDistanceRemaining() {
    return distanceRemaining;
  }

  public void setDistanceRemaining(int distanceRemaining) {
    this.distanceRemaining = distanceRemaining;
  }

  public int getDurationRemaining() {
    return durationRemaining;
  }

  public void setDurationRemaining(int durationRemaining) {
    this.durationRemaining = durationRemaining;
  }

  public int getEventVersion() {
    return eventVersion;
  }

  public void setEventVersion(int eventVersion) {
    this.eventVersion = eventVersion;
  }

  public int getEstimatedDistance() {
    return estimatedDistance;
  }

  public void setEstimatedDistance(int estimatedDistance) {
    this.estimatedDistance = estimatedDistance;
  }

  public int getEstimatedDuration() {
    return estimatedDuration;
  }

  public void setEstimatedDuration(int estimatedDuration) {
    this.estimatedDuration = estimatedDuration;
  }

  public int getRerouteCount() {
    return rerouteCount;
  }

  public void setRerouteCount(int rerouteCount) {
    this.rerouteCount = rerouteCount;
  }

  public int getOriginalEstimatedDistance() {
    return originalEstimatedDistance;
  }

  public void setOriginalEstimatedDistance(int originalEstimatedDistance) {
    this.originalEstimatedDistance = originalEstimatedDistance;
  }

  public int getOriginalEstimatedDuration() {
    return originalEstimatedDuration;
  }

  public void setOriginalEstimatedDuration(int originalEstimatedDuration) {
    this.originalEstimatedDuration = originalEstimatedDuration;
  }

  public int getStepCount() {
    return stepCount;
  }

  public void setStepCount(int stepCount) {
    this.stepCount = stepCount;
  }

  public int getOriginalStepCount() {
    return originalStepCount;
  }

  public void setOriginalStepCount(int originalStepCount) {
    this.originalStepCount = originalStepCount;
  }

  public int getLegIndex() {
    return legIndex;
  }

  public void setLegIndex(int legIndex) {
    this.legIndex = legIndex;
  }

  public int getLegCount() {
    return legCount;
  }

  public void setLegCount(int legCount) {
    this.legCount = legCount;
  }

  public int getStepIndex() {
    return stepIndex;
  }

  public void setStepIndex(int stepIndex) {
    this.stepIndex = stepIndex;
  }

  public int getVoiceIndex() {
    return voiceIndex;
  }

  public void setVoiceIndex(int voiceIndex) {
    this.voiceIndex = voiceIndex;
  }

  public int getBannerIndex() {
    return bannerIndex;
  }

  public void setBannerIndex(int bannerIndex) {
    this.bannerIndex = bannerIndex;
  }

  public int getTotalStepCount() {
    return totalStepCount;
  }

  public void setTotalStepCount(int totalStepCount) {
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