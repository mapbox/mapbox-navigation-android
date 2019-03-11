package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;

/**
 * A listener for getting {@link Location} updates as they are
 * received directly from the {@link com.mapbox.android.core.location.LocationEngine}
 * running in {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
 */
public interface RawLocationListener {

  /**
   * Invoked as soon as a new {@link Location} has been received.
   *
   * @param rawLocation un-snapped update
   */
  void onLocationUpdate(Location rawLocation);
}