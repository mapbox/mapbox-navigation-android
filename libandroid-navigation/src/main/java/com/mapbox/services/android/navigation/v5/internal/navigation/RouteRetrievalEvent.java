package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.annotation.SuppressLint;
import android.os.Parcelable;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class RouteRetrievalEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String ELAPSED_TIME_NAME = "elapsed_time";
  private static final String ROUTE_UUID_NAME = "route_uuid";
  private static final String ROUTE_RETRIEVAL_EVENT_NAME = "route_retrieval_event";

  RouteRetrievalEvent(double elapsedTime, String routeUuid, String sessionId,
                      NavigationPerformanceMetadata metadata) {
    super(sessionId, ROUTE_RETRIEVAL_EVENT_NAME, metadata);

    addCounter(new DoubleCounter(ELAPSED_TIME_NAME, elapsedTime));
    addAttribute(new Attribute(ROUTE_UUID_NAME, routeUuid));
  }
}
