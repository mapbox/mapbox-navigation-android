package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
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
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    // Second update is 100 meters away from the step --> off-route
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 100, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      secondUpdate, routeProgress, options, distances, mockCallback
    );
    assertTrue(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOnStep() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();

    // First update sets the last re-route location
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options, distances, mockCallback);

    // Second update is 10 meters away from the step --> is not off-route
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 10, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      secondUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenWithinRadiusAndStepLocationHasBadAccuracy() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();

    // First update sets the last re-route location
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options, distances, mockCallback);

    // Second update is 250m away from the step, but 300m accuracy --> not off-route
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 250, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());
    secondUpdate.setAccuracy(300f);

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      secondUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOffRouteButCloseToUpcomingStep() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();
    Point upcomingStepManeuverPoint = routeProgress.currentLegProgress().upComingStep().maneuver().location();

    // First update sets the last re-route location
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options, distances, mockCallback);

    // Second update is 30m away from the upcoming step
    // --> considered off-route of the current step
    // --> but not actually off-route because close to upcoming step
    Point offRoutePoint = buildPointAwayFromPoint(upcomingStepManeuverPoint, 30, 180);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(
      secondUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRoute);
    // Also verify the callback to increase the step index is called
    verify(mockCallback, times(1)).onShouldIncreaseIndex();
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenOnRouteButMovingAwayFromManeuver() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    // Get the lineString and corresponding coordinates from the step geometry
    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    // First update sets the last re-route location
    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options, distances, mockCallback);

    // Second update is on current step (1) --> not off-route
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(
      secondLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRouteFirstTry);

    // Third update is on current step, moving away from maneuver (2) --> still not off-route
    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(
      thirdLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRouteSecondTry);

    // Fourth update is on current step, moving away from maneuver (3) --> still not off-route
    Point thirdLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      thirdLastPointInCurrentStep.longitude(), thirdLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(
      fourthLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRouteThirdTry);

    // Fifth update is on current step, but now we have >= 3 updates away from the maneuver --> off-route
    Point fourthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fifthLocationUpdate = buildDefaultLocationUpdate(
      fourthLastPointInCurrentStep.longitude(), fourthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFourthTry = offRouteDetector.isUserOffRoute(
      fifthLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertTrue(isUserOffRouteFourthTry);
  }

  @Test
  public void isUserOffRoute_AssertFalseTwoUpdatesAwayFromManeuverThenOneTowards() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    // Get the lineString and corresponding coordinates from the step geometry
    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    // First update sets the last re-route location
    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523,38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options, distances, mockCallback);

    // Second update is on current step (1) --> not off-route
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(
      secondLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRouteFirstTry);

    // Third update is on current step, moving away from maneuver (2) --> still not off-route
    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(
      thirdLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRouteSecondTry);

    // Fourth update is on current step, moving towards the maneuver, clear the stack --> not off-route
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(
      fourthLocationUpdate, routeProgress, options, distances, mockCallback
    );
    assertFalse(isUserOffRouteThirdTry);
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
  private static Point buildPointAwayFromPoint(Point point, double distanceAway, double bearing) {
    return TurfMeasurement.destination(point, distanceAway, bearing, TurfConstants.UNIT_METERS);
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
