package com.mapbox.navigation.ui.internal.junction;

public class RouteJunction {
  private String guidanceImageUrl;
  private double distanceToHideView;

  public RouteJunction(String guidanceImageUrl, double distanceToHideView) {
    this.guidanceImageUrl = guidanceImageUrl;
    this.distanceToHideView = distanceToHideView;
  }

  public String getGuidanceImageUrl() {
    return guidanceImageUrl;
  }

  public double getDistanceToHideView() {
    return distanceToHideView;
  }
}
