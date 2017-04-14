package com.mapbox.services.android.navigation.v5.listeners;

import com.mapbox.services.Experimental;

/**
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 */
@Experimental
public interface NavigationEventListener {
  void onRunning(boolean running);
}
