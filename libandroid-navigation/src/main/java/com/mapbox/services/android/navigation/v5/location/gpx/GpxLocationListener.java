package com.mapbox.services.android.navigation.v5.location.gpx;

import android.location.Location;

/**
 * Listener that can be added to {@link GpxAnimator} to receive {@link Location}
 * updates when the animator is active.
 *
 * @since 0.3.0
 */
public interface GpxLocationListener {

  void onLocationUpdate(Location gpxLocation);
}
