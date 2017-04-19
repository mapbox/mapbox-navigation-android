package com.mapbox.services.android.navigation.v5;

import com.google.gson.Gson;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import org.junit.Before;

import java.io.IOException;


public class LocationUpdatedThreadTest extends BaseTest {

  private static final String NAVIGATION_FIXTURE = "navigation.json";

  private DirectionsRoute route;

  @Before
  public void before() throws IOException, ServicesException {
    DirectionsResponse response = new Gson().fromJson(readPath(NAVIGATION_FIXTURE), DirectionsResponse.class);
    route = response.getRoutes().get(0);
  }

  //  @Test
  //  public void whenUserStarts_DepartAlertOccurs() throws Exception {
  //    Handler aHandler = mock(Handler.class);
  //    LocationUpdatedThread locationUpdatedThread = new LocationUpdatedThread(aHandler);
  //    Position aTruePosition = mock(Position.class);
  //    Position aSnappedPosition = mock(Position.class);
  //
  //    RouteProgress aRouteProgress = mock(RouteProgress.class);
  //    when(aRouteProgress.getRoute()).thenReturn(route);
  //    when(aRouteProgress.getLegIndex()).thenReturn(0);
  //    when(aRouteProgress.getUpComingStep()).thenReturn(route.getLegs().get(0).getSteps().get(1));
  //
  //    Location location = mock(Location.class);
  //
  //    int alert = locationUpdatedThread.monitorStepProgress(
  // aRouteProgress, location, aTruePosition, aSnappedPosition);
  //    assertEquals(DEPART_ALERT_LEVEL, alert);
  //  }

  //  @Test
  //  public void checksAlertLowLevel() throws Exception {
  //    Handler aHandler = mock(Handler.class);
  //    LocationUpdatedThread locationUpdatedThread = new LocationUpdatedThread(aHandler);
  //
  //    RouteProgress aRouteProgress = mock(RouteProgress.class);
  //    when(aRouteProgress.getRoute()).thenReturn(route);
  //    when(aRouteProgress.getLegIndex()).thenReturn(0);
  //    when(aRouteProgress.getCurrentStep()).thenReturn(route.getLegs().get(0).getSteps().get(0));
  //    when(aRouteProgress.getPreviousAlertLevel()).thenReturn(DEPART_ALERT_LEVEL);
  //    when(aRouteProgress.getUpComingStep()).thenReturn(route.getLegs().get(0).getSteps().get(1));
  //
  //    LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_5);
  //    Point userLocation = TurfMeasurement.along(lineString, 10, TurfConstants.UNIT_METERS);
  //
  //    Location location = mock(Location.class);
  //    when(location.getLatitude()).thenReturn(userLocation.getCoordinates().getLatitude());
  //    when(location.getLongitude()).thenReturn(userLocation.getCoordinates().getLongitude());
  //
  //    Position truePosition = Position.fromCoordinates(
  //      userLocation.getCoordinates().getLongitude(), userLocation.getCoordinates().getLatitude());
  //
  //    Position snappedPosition = RouteUtils.getSnapToRoute(
  //      truePosition,
  //      route.getLegs().get(0), 0);
  //
  //    assertEquals(LOW_ALERT_LEVEL,
  // locationUpdatedThread.monitorStepProgress(aRouteProgress, location, truePosition, snappedPosition));
  //  }
}