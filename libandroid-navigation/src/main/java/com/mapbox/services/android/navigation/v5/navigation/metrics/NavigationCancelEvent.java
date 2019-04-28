package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;

@SuppressLint("ParcelCreator")
class NavigationCancelEvent extends NavigationEvent {
  private static final String NAVIGATION_CANCEL = "navigation.cancel";
  private String arrivalTimestamp;
  private int rating;
  private String comment;

  NavigationCancelEvent(PhoneState phoneState) {
    super(phoneState);
  }

  @Override
  String getEventName() {
    return NAVIGATION_CANCEL;
  }

  String getArrivalTimestamp() {
    return arrivalTimestamp;
  }

  void setArrivalTimestamp(String arrivalTimestamp) {
    this.arrivalTimestamp = arrivalTimestamp;
  }

  int getRating() {
    return rating;
  }

  void setRating(Integer rating) {
    this.rating = rating;
  }

  String getComment() {
    return comment;
  }

  void setComment(String comment) {
    this.comment = comment;
  }
}
