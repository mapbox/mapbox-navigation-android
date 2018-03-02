package com.mapbox.services.android.navigation.ui.v5.camera;

import android.location.Location;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class DynamicCameraTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Mock
  private MapboxMap mapboxMap;

  private DynamicCamera cameraEngine;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    cameraEngine = new DynamicCamera(mapboxMap);
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(cameraEngine);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectTarget() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);
    Point target = cameraEngine.target(routeInformation);
    double lng = target.longitude();
    assertEquals(-122.416686, lng);
    double lat = target.latitude();
    assertEquals(37.783425, lat);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectZoom() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);
    double zoom = cameraEngine.zoom(routeInformation);
    assertEquals(15d, zoom);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectTilt() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);
    double tilt = cameraEngine.tilt(routeInformation);
    assertEquals(50d, tilt);
  }

  @Test
  public void onInformationFromRoute_engineCreatesCorrectBearing() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(buildDirectionsRoute(), null, null);
    double bearing = cameraEngine.bearing(routeInformation);
    assertEquals(-99, Math.round(bearing));
  }

  @Test
  public void onInformationFromLocationAndProgress_engineCreatesCorrectTarget() throws Exception {
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
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(1000d));
    double tilt = cameraEngine.tilt(routeInformation);
    assertEquals(50d, tilt);
  }

  @Test
  public void onMediumDistanceRemaining_engineCreatesCorrectTilt() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(200d));
    double tilt = cameraEngine.tilt(routeInformation);
    assertEquals(40d, tilt);
  }

  @Test
  public void onLowDistanceRemaining_engineCreatesCorrectTilt() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(null));
    double tilt = cameraEngine.tilt(routeInformation);
    assertEquals(35d, tilt);
  }

  @Test
  public void onInformationFromLocationAndProgress_engineCreatesCorrectBearing() throws Exception {
    RouteInformation routeInformation = RouteInformation.create(null,
      buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637), buildDefaultRouteProgress(null));
    double bearing = cameraEngine.bearing(routeInformation);
    assertEquals(100f, bearing, DELTA);
  }

  private Location buildDefaultLocationUpdate(double lng, double lat) {
    return buildLocationUpdate(lng, lat, System.currentTimeMillis());
  }

  private Location buildLocationUpdate(double lng, double lat, long time) {
    Location location = new Location(DynamicCameraTest.class.getSimpleName());
    location.setLongitude(lng);
    location.setLatitude(lat);
    location.setSpeed(30f);
    location.setBearing(100f);
    location.setAccuracy(10f);
    location.setTime(time);
    return location;
  }

  private RouteProgress buildDefaultRouteProgress(@Nullable Double stepDistanceRemaining) throws Exception {
    DirectionsRoute aRoute = buildDirectionsRoute();
    return RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining == null ? 100 : stepDistanceRemaining)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(aRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }
}
