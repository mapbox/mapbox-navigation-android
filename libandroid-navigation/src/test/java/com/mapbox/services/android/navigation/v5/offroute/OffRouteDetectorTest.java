package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class OffRouteDetectorTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  private OffRoute offRouteDetector;
  private MapboxNavigationOptions options;
  private RingBuffer<Integer> distances;

  @Mock
  Location mockLocation;
  @Mock
  RouteProgress mockProgress;
  @Mock
  OffRouteCallback mockCallback;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    offRouteDetector = new OffRouteDetector();
    options = MapboxNavigationOptions.builder().build();
    distances = new RingBuffer<>(3);
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(offRouteDetector);
  }

  @Test
  public void invalidOffRoute_onFirstLocationUpdate() throws Exception {
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      mockLocation, mockProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRoute);
  }

  @Test
  public void validOffRoute_onMinimumDistanceBeforeReroutingPassed() throws Exception {
    Location mapboxOffice = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    RouteProgress routeProgress = buildDefaultRouteProgress();

    // First update sets the last re-route location
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      mapboxOffice, mockProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRoute);

    // Second update 1 meter greater than minimum distance before re-routing
    Point target = buildPointAwayFromLocation(mapboxOffice, options.minimumDistanceBeforeRerouting() + 1);

    Location locationOverMinimumDistance = buildDefaultLocationUpdate(target.longitude(), target.latitude());
    // Location is valid 21 meters away from mapboxOffice location
    boolean validOffRoute = offRouteDetector.isUserOffRoute(
      locationOverMinimumDistance, routeProgress, options, distances, mockCallback
    );
    assertTrue(validOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenTooFarFromStep() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();

    // First update sets the last re-route location
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options, distances, mockCallback);

    // Second update is 100 meters away from the step --> off route
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 100);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      secondUpdate, routeProgress, options, distances, mockCallback
    );
    assertTrue(isUserOffRoute);
  }

  /**
   * @return {@link Location} with Mapbox DC coordinates
   */
  private static Location buildDefaultLocationUpdate(double lng, double lat) {
    return buildLocationUpdate(lng,lat, 30f, 10f, System.currentTimeMillis());
  }

  private static Location buildLocationUpdate(double lng, double lat, float speed, float horizontalAccuracy, long time) {
    Location location = new Location(OffRouteDetectorTest.class.getSimpleName());
    location.setLongitude(lng);
    location.setLatitude(lat);
    location.setSpeed(speed);
    location.setAccuracy(horizontalAccuracy);
    location.setTime(time);
    return location;
  }

  @NonNull
  private static Point buildPointAwayFromLocation(Location location, double distanceAway) {
    Point fromLocation = Point.fromLngLat(
      location.getLongitude(), location.getLatitude());
    return TurfMeasurement.destination(fromLocation, distanceAway, 90, TurfConstants.UNIT_METERS);
  }

  @NonNull
  private static Point buildPointAwayFromPoint(Point point, double distanceAway) {
    return TurfMeasurement.destination(point, distanceAway, 90, TurfConstants.UNIT_METERS);
  }

  private RouteProgress buildDefaultRouteProgress() throws Exception {
    DirectionsRoute aRoute = buildDirectionsRoute();
    RouteProgress defaultRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(aRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();

    return defaultRouteProgress;
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute aRoute = response.routes().get(0);

    return aRoute;
  }
}
