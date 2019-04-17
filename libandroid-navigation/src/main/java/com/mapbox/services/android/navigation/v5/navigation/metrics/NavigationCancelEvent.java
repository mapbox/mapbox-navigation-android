package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;

@SuppressLint("ParcelCreator")
public class NavigationCancelEvent extends NavigationEvent {
  private static final String NAVIGATION_CANCEL = "navigation.cancel";
  private String arrivalTimestamp;
  private int rating;
  private String comment;

  NavigationCancelEvent(PhoneState phoneState) {
    super(phoneState);
  }

  @Override
  protected String getEventName() {
    return NAVIGATION_CANCEL;
  }

  public String getArrivalTimestamp() {
    return arrivalTimestamp;
  }

  public void setArrivalTimestamp(String arrivalTimestamp) {
    this.arrivalTimestamp = arrivalTimestamp;
  }

  public int getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
