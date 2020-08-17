package com.mapbox.navigation.ui;

import androidx.annotation.NonNull;

import java.util.Date;

class RouteCallStatus {

  private static final int FIVE_SECONDS_IN_MILLISECONDS = 5000;
  private boolean responseReceived;
  private final Date callDate;

  RouteCallStatus(Date callDate) {
    this.callDate = callDate;
  }

  void setResponseReceived() {
    this.responseReceived = true;
  }

  boolean isRouting(@NonNull Date currentDate) {
    if (responseReceived) {
      return false;
    }
    return diffInMilliseconds(callDate, currentDate) < FIVE_SECONDS_IN_MILLISECONDS;
  }

  private long diffInMilliseconds(@NonNull Date callDate, @NonNull Date currentDate) {
    return currentDate.getTime() - callDate.getTime();
  }
}