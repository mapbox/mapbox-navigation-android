package com.mapbox.services.android.navigation.v5.routeprogress;

import android.location.Location;

import com.mapbox.services.Experimental;

public interface ProgressChangeListener {
  void onProgressChange(Location location, RouteProgress routeProgress);
}
