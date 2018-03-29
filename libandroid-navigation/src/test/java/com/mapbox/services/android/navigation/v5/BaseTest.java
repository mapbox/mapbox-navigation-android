package com.mapbox.services.android.navigation.v5;

import android.location.Location;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static junit.framework.Assert.assertEquals;
import static okhttp3.internal.Util.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseTest {

  public static final double DELTA = 1E-10;
  public static final double LARGE_DELTA = 0.1;

  public static final String ACCESS_TOKEN = "pk.XXX";
  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  public void compareJson(String json1, String json2) {
    JsonParser parser = new JsonParser();
    assertEquals(parser.parse(json1), parser.parse(json2));
  }

  protected String loadJsonFixture(String filename) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filename);
    Scanner scanner = new Scanner(inputStream, UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }

  protected RouteProgress buildDefaultRouteProgress() throws Exception {
    DirectionsRoute aRoute = buildDirectionsRoute();
    List<Point> currentStepPoints = PolylineUtils.decode(aRoute.legs().get(0).steps().get(0).geometry(), PRECISION_6);
    RouteProgress defaultRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(aRoute)
      .currentStepPoints(currentStepPoints)
      .stepIndex(0)
      .legIndex(0)
      .build();

    return defaultRouteProgress;
  }

  protected DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute aRoute = response.routes().get(0);

    return aRoute;
  }

  protected Location buildDefaultLocationUpdate(double lng, double lat) {
    return buildLocationUpdate(lng, lat, 30f, 10f, System.currentTimeMillis());
  }

  protected Location buildLocationUpdate(double lng, double lat, float speed, float horizontalAccuracy, long time) {
    Location location = mock(Location.class);
    when(location.getLongitude()).thenReturn(lng);
    when(location.getLatitude()).thenReturn(lat);
    when(location.getSpeed()).thenReturn(speed);
    when(location.getAccuracy()).thenReturn(horizontalAccuracy);
    when(location.getTime()).thenReturn(time);
    return location;
  }

  @NonNull
  protected Point buildPointAwayFromLocation(Location location, double distanceAway) {
    Point fromLocation = Point.fromLngLat(
      location.getLongitude(), location.getLatitude());
    return TurfMeasurement.destination(fromLocation, distanceAway, 90, TurfConstants.UNIT_METERS);
  }

  @NonNull
  protected Point buildPointAwayFromPoint(Point point, double distanceAway, double bearing) {
    return TurfMeasurement.destination(point, distanceAway, bearing, TurfConstants.UNIT_METERS);
  }

  @NonNull
  protected List<Point> createCoordinatesFromCurrentStep(RouteProgress progress) {
    LegStep currentStep = progress.currentLegProgress().currentStep();
    LineString lineString = LineString.fromPolyline(currentStep.geometry(), PRECISION_6);
    return lineString.coordinates();
  }

  protected LegStep getFirstStep(DirectionsRoute route) {
    return route.legs().get(0).steps().get(0);
  }

  protected List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}
