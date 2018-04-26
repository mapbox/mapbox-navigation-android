package com.mapbox.services.android.navigation.ui.v5.location;

import android.location.Location;

public interface NavigationLocationEngineListener {

  void onLocationUpdate(Location location);
}
