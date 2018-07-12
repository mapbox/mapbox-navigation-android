package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildSnappedLocation;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationRouteProcessorTest extends BaseTest {

  private NavigationRouteProcessor routeProcessor;
  private MapboxNavigation navigation;

  @Before
  public void before() throws Exception {
    routeProcessor = new NavigationRouteProcessor();
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    Context context = mock(Context.class);
    when(context.getApplicationContext()).thenReturn(context);
    navigation = new MapboxNavigation(context, ACCESS_TOKEN, options, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
    navigation.startNavigation(buildTestDirectionsRoute());
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(routeProcessor);
  }

  @Test
  public void onFirstRouteProgressBuilt_newRouteIsDecoded() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    assertEquals(0, progress.legIndex());
    assertEquals(0, progress.currentLegProgress().stepIndex());
  }

  @Test
  public void onShouldIncreaseStepIndex_indexIsIncreased() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int currentStepIndex = progress.currentLegProgress().stepIndex();
    routeProcessor.onShouldIncreaseIndex();
    routeProcessor.checkIncreaseIndex(navigation);

    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int secondStepIndex = secondProgress.currentLegProgress().stepIndex();

    assertTrue(currentStepIndex != secondStepIndex);
  }

  @Test
  public void onSnapToRouteEnabledAndUserOnRoute_snappedLocationReturns() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    boolean snapEnabled = true;
    boolean userOffRoute = false;
    List<Point> coordinates = createCoordinatesFromCurrentStep(progress);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    Location snappedLocation = buildSnappedLocation(
      navigation, snapEnabled, rawLocation, progress, userOffRoute
    );

    assertTrue(!rawLocation.equals(snappedLocation));
  }

  @Test
  public void onSnapToRouteDisabledAndUserOnRoute_rawLocationReturns() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    boolean snapEnabled = false;
    boolean userOffRoute = false;
    List<Point> coordinates = createCoordinatesFromCurrentStep(progress);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    Location snappedLocation = buildSnappedLocation(
      navigation, snapEnabled, rawLocation, progress, userOffRoute
    );

    assertTrue(rawLocation.equals(snappedLocation));
  }

  @Test
  public void onSnapToRouteEnabledAndUserOffRoute_rawLocationReturns() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    boolean snapEnabled = false;
    boolean userOffRoute = false;
    List<Point> coordinates = createCoordinatesFromCurrentStep(progress);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    Location snappedLocation = buildSnappedLocation(
      navigation, snapEnabled, rawLocation, progress, userOffRoute
    );

    assertTrue(rawLocation.equals(snappedLocation));
  }

  @Test
  public void onStepDistanceRemainingZeroAndNoBearingMatch_stepIndexForceIncreased() throws Exception {
    RouteProgress firstProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int firstProgressIndex = firstProgress.currentLegProgress().stepIndex();
    List<Point> coordinates = createCoordinatesFromCurrentStep(firstProgress);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, rawLocation);
    int secondProgressIndex = secondProgress.currentLegProgress().stepIndex();

    assertTrue(firstProgressIndex != secondProgressIndex);
  }

  @Test
  public void onInvalidNextLeg_indexIsNotIncreased() throws Exception {
    routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int legSize = navigation.getRoute().legs().size();

    for (int i = 0; i < legSize; i++) {
      routeProcessor.onShouldIncreaseIndex();
      routeProcessor.checkIncreaseIndex(navigation);
    }
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));

    assertTrue(progress.legIndex() == legSize - 1);
  }

  @Test
  public void onInvalidNextStep_indexIsNotIncreased() throws Exception {
    routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int stepSize = navigation.getRoute().legs().get(0).steps().size();

    for (int i = 0; i < stepSize; i++) {
      routeProcessor.onShouldIncreaseIndex();
      routeProcessor.checkIncreaseIndex(navigation);
    }
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));

    assertTrue(progress.currentLegProgress().stepIndex() == stepSize - 1);
  }

  @Test
  public void withinManeuverRadiusAndBearingMatches_stepIndexIsIncreased() throws Exception {
    RouteProgress firstProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int firstProgressIndex = firstProgress.currentLegProgress().stepIndex();
    List<Point> coordinates = createCoordinatesFromCurrentStep(firstProgress);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    when(rawLocation.getBearing()).thenReturn(145f);

    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, rawLocation);
    int secondProgressIndex = secondProgress.currentLegProgress().stepIndex();

    assertTrue(firstProgressIndex != secondProgressIndex);
  }
}
