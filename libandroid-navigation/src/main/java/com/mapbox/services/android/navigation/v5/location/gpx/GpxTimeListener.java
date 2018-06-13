package com.mapbox.services.android.navigation.v5.location.gpx;

import android.animation.TimeAnimator;
import android.location.Location;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
class GpxTimeListener implements TimeAnimator.TimeListener {

  private static final int FIRST_LOCATION = 0;

  private final List<GpxLocationListener> listeners;
  private final List<Location> gpxLocations;
  private final long startTime;

  GpxTimeListener(List<GpxLocationListener> listeners, List<Location> gpxLocations) {
    this.listeners = listeners;
    this.gpxLocations = gpxLocations;

    Location location = gpxLocations.remove(FIRST_LOCATION);
    startTime = location.getTime();
    sendLocationUpdate(location);
  }

  @Override
  public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
    if (!gpxLocations.isEmpty()) {
      Location location = gpxLocations.get(FIRST_LOCATION);
      long nextLocationTime = location.getTime();
      long diff = nextLocationTime - startTime;
      if (totalTime > diff) {
        sendLocationUpdate(location);
        gpxLocations.remove(location);
      }
    }
  }

  private void sendLocationUpdate(Location gpxLocation) {
    for (GpxLocationListener listener : listeners) {
      listener.onLocationUpdate(gpxLocation);
    }
  }
}
