package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationRouteProcessorTest extends BaseTest {

  private NavigationRouteProcessor routeProcessor;
  private MapboxNavigation navigation;

  @Before
  public void setup() throws Exception {
    routeProcessor = new NavigationRouteProcessor();
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    navigation = new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, options, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
    navigation.startNavigation(buildDirectionsRoute());
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
    routeProcessor.checkIncreaseStepIndex(navigation);
    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int secondStepIndex = secondProgress.currentLegProgress().stepIndex();
    assertTrue(currentStepIndex != secondStepIndex);
  }

  @Test
  public void onSnapToRouteEnabledAndUserOnRoute_snappedLocationReturns() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));

    boolean snapEnabled = true;
    boolean userOffRoute = false;

    LegStep currentStep = progress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    Location snappedLocation = routeProcessor.buildSnappedLocation(
      navigation, snapEnabled, rawLocation, progress, userOffRoute
    );

    assertTrue(!rawLocation.equals(snappedLocation));
  }

  @Test
  public void onSnapToRouteDisabledAndUserOnRoute_rawLocationReturns() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));

    boolean snapEnabled = false;
    boolean userOffRoute = false;

    LegStep currentStep = progress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    Location snappedLocation = routeProcessor.buildSnappedLocation(
      navigation, snapEnabled, rawLocation, progress, userOffRoute
    );

    assertTrue(rawLocation.equals(snappedLocation));
  }

  @Test
  public void onSnapToRouteEnabledAndUserOffRoute_rawLocationReturns() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));

    boolean snapEnabled = false;
    boolean userOffRoute = false;

    LegStep currentStep = progress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    Location snappedLocation = routeProcessor.buildSnappedLocation(
      navigation, snapEnabled, rawLocation, progress, userOffRoute
    );

    assertTrue(rawLocation.equals(snappedLocation));
  }

  @Test
  public void onStepDistanceRemainingZeroAndNoBearingMatch_stepIndexForceIncreased() throws Exception {
    RouteProgress firstProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int firstProgressIndex = firstProgress.currentLegProgress().stepIndex();

    LegStep currentStep = firstProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );

    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, rawLocation);
    int secondProgressIndex = secondProgress.currentLegProgress().stepIndex();

    assertTrue(firstProgressIndex != secondProgressIndex);
  }

  @Test
  public void withinManeuverRadiusAndBearingMatches_stepIndexIsIncreased() throws Exception {
    RouteProgress firstProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int firstProgressIndex = firstProgress.currentLegProgress().stepIndex();

    LegStep currentStep = firstProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 2);
    Location rawLocation = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    rawLocation.setBearing(145f);

    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, rawLocation);
    int secondProgressIndex = secondProgress.currentLegProgress().stepIndex();

    assertTrue(firstProgressIndex != secondProgressIndex);
  }
}
