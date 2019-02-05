package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.os.Parcelable;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class RouteRetrievalEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String ELAPSED_TIME_NAME = "elapsed_time";
  private static final String ROUTE_UUID_NAME = "route_uuid";

  RouteRetrievalEvent(long elapsedTime, String routeUuid, String sessionId) {
    super(sessionId);

    addCounter(new LongCounter(ELAPSED_TIME_NAME, elapsedTime));
    addAttribute(new Attribute(ROUTE_UUID_NAME, routeUuid));
  }
}
