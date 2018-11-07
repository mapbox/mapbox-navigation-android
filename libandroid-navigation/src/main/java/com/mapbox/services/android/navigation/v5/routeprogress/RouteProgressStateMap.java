package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.navigator.RouteState;

import java.util.HashMap;

public final class RouteProgressStateMap extends HashMap<RouteState, RouteProgressState> {

  public RouteProgressStateMap() {
    put(RouteState.INVALID, RouteProgressState.ROUTE_INVALID);
    put(RouteState.INITIALIZED, RouteProgressState.ROUTE_INITIALIZED);
    put(RouteState.COMPLETE, RouteProgressState.ROUTE_ARRIVED);
    put(RouteState.TRACKING, RouteProgressState.LOCATION_TRACKING);
    put(RouteState.STALE, RouteProgressState.LOCATION_STALE);
    put(RouteState.OFFROUTE, null); // Ignore off-route (info already provided via listener)
  }
}
