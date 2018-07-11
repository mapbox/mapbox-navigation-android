package com.mapbox.services.android.navigation.v5.location.replay;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

class ReplayLocationDto {

  @SerializedName("lng")
  private double longitude;
  @SerializedName("horizontalAccuracy")
  private float horizontalAccuracyMeters;
  @SerializedName("course")
  private double bearing;
  @SerializedName("verticalAccuracy")
  private float verticalAccuracyMeters;
  private double speed;
  @SerializedName("lat")
  private double latitude;
  @SerializedName("altitude")
  private double altitude;
  @SerializedName("timestamp")
  @JsonAdapter(TimestampAdapter.class)
  private Date date;

  double getLongitude() {
    return longitude;
  }

  void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  float getHorizontalAccuracyMeters() {
    return horizontalAccuracyMeters;
  }

  void setHorizontalAccuracyMeters(float horizontalAccuracyMeters) {
    this.horizontalAccuracyMeters = horizontalAccuracyMeters;
  }

  double getBearing() {
    return bearing;
  }

  void setBearing(double bearing) {
    this.bearing = bearing;
  }

  float getVerticalAccuracyMeters() {
    return verticalAccuracyMeters;
  }

  void setVerticalAccuracyMeters(float verticalAccuracyMeters) {
    this.verticalAccuracyMeters = verticalAccuracyMeters;
  }

  double getSpeed() {
    return speed;
  }

  void setSpeed(double speed) {
    this.speed = speed;
  }

  double getLatitude() {
    return latitude;
  }

  void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  double getAltitude() {
    return altitude;
  }

  void setAltitude(double altitude) {
    this.altitude = altitude;
  }

  Date getDate() {
    return date;
  }

  void setDate(Date date) {
    this.date = date;
  }
}
