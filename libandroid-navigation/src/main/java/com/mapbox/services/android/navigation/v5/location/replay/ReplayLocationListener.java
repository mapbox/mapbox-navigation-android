package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;


interface ReplayLocationListener {

  void onLocationReplay(Location location);
}
