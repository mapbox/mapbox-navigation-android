package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;
import android.os.Build;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ReplayJsonRouteLocationMapper {

  private static final String NON_NULL_AND_NON_EMPTY_REPLAY_LOCATION_LIST_REQUIRED = "Non-null and non-empty replay "
    + "location list required.";
  private static final String REPLAY = "ReplayLocation";
  private final List<ReplayLocationDto> replayLocations;

  ReplayJsonRouteLocationMapper(List<ReplayLocationDto> replayLocations) {
    checkValidInput(replayLocations);
    this.replayLocations = replayLocations;
  }

  List<Location> toLocations() {
    List<Location> mappedLocations = mapReplayLocations();
    return mappedLocations;
  }

  private void checkValidInput(List<ReplayLocationDto> locations) {
    boolean isValidInput = locations == null || locations.isEmpty();
    if (isValidInput) {
      throw new IllegalArgumentException(NON_NULL_AND_NON_EMPTY_REPLAY_LOCATION_LIST_REQUIRED);
    }
  }

  private List<Location> mapReplayLocations() {
    List<Location> locations = new ArrayList<>(replayLocations.size());
    for (ReplayLocationDto sample : replayLocations) {
      Location location = new Location(REPLAY);
      location.setLongitude(sample.getLongitude());
      location.setAccuracy(sample.getHorizontalAccuracyMeters());
      location.setBearing((float) sample.getBearing());
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        location.setVerticalAccuracyMeters(sample.getVerticalAccuracyMeters());
      }
      location.setSpeed((float) sample.getSpeed());
      location.setLatitude(sample.getLatitude());
      location.setAltitude(sample.getAltitude());
      Date date = sample.getDate();
      if (date != null) {
        location.setTime(date.getTime());
      }
      locations.add(location);
    }
    return locations;
  }
}
