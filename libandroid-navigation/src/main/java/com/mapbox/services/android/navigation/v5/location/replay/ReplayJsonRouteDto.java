package com.mapbox.services.android.navigation.v5.location.replay;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class ReplayJsonRouteDto {

  private List<ReplayLocationDto> locations;
  @SerializedName("route")
  private String routeRequest;

  List<ReplayLocationDto> getLocations() {
    return locations;
  }

  void setLocations(List<ReplayLocationDto> locations) {
    this.locations = locations;
  }

  String getRouteRequest() {
    return routeRequest;
  }

  void setRouteRequest(String routeRequest) {
    this.routeRequest = routeRequest;
  }
}
