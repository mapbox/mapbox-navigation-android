package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.NonNull;

/**
 * A listener for getting the best enhanced {@link Location} updates available at any
 * moment. Either snapped (active guidance), map matched (free drive) or raw.
 * <p>
 * The behavior that causes this listeners callback to get invoked vary depending on whether
 * free drive has been enabled using {@link MapboxNavigation#enableFreeDrive()} or disabled using
 * {@link MapboxNavigation#disableFreeDrive}.
 *
 * @see MapboxNavigation#enableFreeDrive()
 */
public interface EnhancedLocationListener {
  /**
   * Invoked as soon as a new {@link Location} has been received.
   *
   * @param enhancedLocation either snapped (active guidance), map matched (free drive) or raw
   */
  void onEnhancedLocationUpdate(@NonNull Location enhancedLocation);
}
