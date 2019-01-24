package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcelable;

@SuppressWarnings("ParcelableCreator")
class RouteRetrievalEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String ELAPSED_TIME_NAME = "elapsed_time";
  private static final String DISTANCE_NAME = "distance";
  private static final String STEP_COUNT_NAME = "step_count";
  private static final String COORDINATE_COUNT_NAME = "coordinate_count";
  private static final String NUMBER_OF_ROUTES_NAME = "number_of_routes";
  private static final String PROFILE_NAME = "profile";
  private static final String IS_OFFLINE_NAME = "is_offline";

  RouteRetrievalEvent(RouteRetrievalInfo routeRetrievalInfo, String sessionId) {
    super(sessionId);

    addCounter(new LongCounter(ELAPSED_TIME_NAME, routeRetrievalInfo.elapsedTime()));
    addCounter(new DoubleCounter(DISTANCE_NAME, routeRetrievalInfo.distance()));
    addCounter(new IntCounter(STEP_COUNT_NAME, routeRetrievalInfo.stepCount()));
    addCounter(new IntCounter(COORDINATE_COUNT_NAME, routeRetrievalInfo.coordinateCount()));
    addCounter(new IntCounter(NUMBER_OF_ROUTES_NAME, routeRetrievalInfo.numberOfRoutes()));
    addAttribute(new Attribute(PROFILE_NAME, routeRetrievalInfo.profile()));
    addAttribute(new Attribute(IS_OFFLINE_NAME, Boolean.toString(routeRetrievalInfo.isOffline())));
  }
}
