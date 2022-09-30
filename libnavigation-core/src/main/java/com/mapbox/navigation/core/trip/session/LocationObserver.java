package com.mapbox.navigation.core.trip.session;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

/**
 * An interface which enables listening to location updates
 *
 * @see LocationMatcherResult
 */
@UiThread
public interface LocationObserver {

  /**
   * Invoked as soon as a new [Location] has been received.
   *
   * @param rawLocation un-snapped update
   */
  default void onNewRawLocation(@NonNull Location rawLocation) {
    // Override to capture
  }

  /**
   * Provides the best possible location update, snapped to the route or map-matched to the road if possible.
   *
   * @param locationMatcherResult details about the status of the enhanced location.
   */
  default void onNewLocationMatcherResult(@NonNull LocationMatcherResult locationMatcherResult) {
    // Override to capture
  }
}
