package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.commons.models.Position;

import java.util.List;

public abstract class OffRoute {

  public abstract boolean isUserOffRoute(Location location, RouteProgress routeProgress,
                                         MapboxNavigationOptions options,
                                         RingBuffer<Integer> recentDistancesFromManeuverInMeters);
}
