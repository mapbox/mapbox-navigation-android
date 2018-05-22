package com.mapbox.services.android.navigation.ui.v5.camera;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicCameraTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() {
    DynamicCamera cameraEngine = buildDynamicCamera();

    assertNotNull(cameraEngine);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectTarget() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);

    Point target = cameraEngine.target(routeInformation);

    double lng = target.longitude();
    assertEquals(-122.416686, lng);
    double lat = target.latitude();
    assertEquals(37.783425, lat);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectZoom() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);

    double zoom = cameraEngine.zoom(routeInformation);

    assertEquals(15d, zoom);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectTilt() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);

    double tilt = cameraEngine.tilt(routeInformation);

    assertEquals(50d, tilt);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectBearing() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);

    double bearing = cameraEngine.bearing(routeInformation);

    assertEquals(-99, Math.round(bearing));
  }

  @Test
  public void onInformationFromLocationAndProgress_engineCreatesCorrectTarget() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(null));

    Point target = cameraEngine.target(routeInformation);

    double lng = target.longitude();
    assertEquals(-77.0339782574523, lng);
    double lat = target.latitude();
    assertEquals(38.89993519985637, lat);
  }

  @Test
  public void onHighDistanceRemaining_engineCreatesCorrectTilt() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(1000d));

    double tilt = cameraEngine.tilt(routeInformation);

    assertEquals(50d, tilt);
  }

  @Test
  public void onMediumDistanceRemaining_engineCreatesCorrectTilt() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(200d));

    double tilt = cameraEngine.tilt(routeInformation);

    assertEquals(40d, tilt);
  }

  @Test
  public void onLowDistanceRemaining_engineCreatesCorrectTilt() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(null));

    double tilt = cameraEngine.tilt(routeInformation);

    assertEquals(35d, tilt);
  }

  @Test
  public void onInformationFromLocationAndProgress_engineCreatesCorrectBearing() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(null));

    double bearing = cameraEngine.bearing(routeInformation);

    assertEquals(100f, bearing, DELTA);
  }

  @Test
  public void onInformationFromRoute_engineCreatesOverviewPointList() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    DirectionsRoute route = buildDirectionsRoute();
    List<Point> routePoints = generateRouteCoordinates(route);
    RouteInformation routeInformation = RouteInformation.create(route, null, null);

    List<Point> overviewPoints = cameraEngine.overview(routeInformation);

    assertEquals(routePoints, overviewPoints);
  }

  @Test
  public void onInformationFromRouteProgress_engineCreatesOverviewPointList() throws Exception {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteProgress routeProgress = buildDefaultRouteProgress(null);
    List<Point> routePoints = buildRouteCoordinatesFrom(routeProgress);
    RouteInformation routeInformation = RouteInformation.create(null, null, routeProgress);

    List<Point> overviewPoints = cameraEngine.overview(routeInformation);

    assertEquals(routePoints, overviewPoints);
  }

  @Test
  public void noRouteInformation_engineCreatesEmptyOverviewPointList() {
    DynamicCamera cameraEngine = buildDynamicCamera();
    RouteInformation routeInformation = RouteInformation.create(null, null, null);

    List<Point> overviewPoints = cameraEngine.overview(routeInformation);

    assertTrue(overviewPoints.isEmpty());
  }

  @Nullable
  private List<Point> buildRouteCoordinatesFrom(RouteProgress routeProgress) {
    DirectionsRoute route = routeProgress.directionsRoute();
    return generateRouteCoordinates(route);
  }

  @NonNull
  private DynamicCamera buildDynamicCamera() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    return new DynamicCamera(mapboxMap);
  }

  private Location buildDefaultLocationUpdate(double lng, double lat) {
    return buildLocationUpdate(lng, lat, System.currentTimeMillis());
  }

  private Location buildLocationUpdate(double lng, double lat, long time) {
    Location location = mock(Location.class);
    when(location.getLongitude()).thenReturn(lng);
    when(location.getLatitude()).thenReturn(lat);
    when(location.getSpeed()).thenReturn(30f);
    when(location.getBearing()).thenReturn(100f);
    when(location.getAccuracy()).thenReturn(10f);
    when(location.getTime()).thenReturn(time);
    return location;
  }

  private RouteProgress buildDefaultRouteProgress(@Nullable Double stepDistanceRemaining) throws Exception {
    DirectionsRoute aRoute = buildDirectionsRoute();
    double stepDistanceRemainingFinal = stepDistanceRemaining == null ? 100 : stepDistanceRemaining;
    return buildRouteProgress(aRoute, stepDistanceRemainingFinal, 0, 0, 0, 0);
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }

  private List<Point> generateRouteCoordinates(DirectionsRoute route) {
    if (route == null) {
      return Collections.emptyList();
    }
    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    return lineString.coordinates();
  }
}
