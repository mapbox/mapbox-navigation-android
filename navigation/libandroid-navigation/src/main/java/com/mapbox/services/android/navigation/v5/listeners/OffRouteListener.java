package com.mapbox.services.android.navigation.v5.listeners;

import android.location.Location;

import com.mapbox.services.Experimental;

/**
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 */
@Experimental
public interface OffRouteListener {
  void userOffRoute(Location location);
}
