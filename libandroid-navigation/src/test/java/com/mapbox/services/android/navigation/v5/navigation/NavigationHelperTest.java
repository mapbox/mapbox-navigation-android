package com.mapbox.services.android.navigation.v5.navigation;

import android.support.v4.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class NavigationHelperTest extends BaseTest {

  private static final String MULTI_LEG_ROUTE_FIXTURE = "directions_two_leg_route.json";
  private static final String ANNOTATED_DISTANCE_CONGESTION_ROUTE_FIXTURE = "directions_distance_congestion_annotation.json";

  @Test
  public void createIntersectionList_returnsCompleteIntersectionList() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    LegStep upcomingStep = routeProgress.currentLegProgress().upComingStep();

    List<StepIntersection> intersections = NavigationHelper.createIntersectionsList(currentStep, upcomingStep);
    int correctListSize = currentStep.intersections().size() + 1;

    assertTrue(correctListSize == intersections.size());
  }

  @Test
  public void createIntersectionList_upcomingStepNull_returnsCurrentStepIntersectionList() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    LegStep upcomingStep = null;

    List<StepIntersection> intersections = NavigationHelper.createIntersectionsList(currentStep, upcomingStep);
    int correctListSize = currentStep.intersections().size() + 1;

    assertFalse(correctListSize == intersections.size());
  }

  @Test
  public void createIntersectionDistanceList_samePointsForDistanceCalculationsEqualZero() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<Point> currentStepPoints = PolylineUtils.decode(currentStep.geometry(), Constants.PRECISION_6);
    List<StepIntersection> currentStepIntersections = currentStep.intersections();

    List<Pair<StepIntersection, Double>> intersectionDistances = NavigationHelper.createDistancesToIntersections(
      currentStepPoints, currentStepIntersections
    );

    assertTrue(intersectionDistances.get(0).second == 0);
  }

  @Test
  public void createIntersectionDistanceList_intersectionListSizeEqualsDistanceListSize() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<Point> currentStepPoints = PolylineUtils.decode(currentStep.geometry(), Constants.PRECISION_6);
    List<StepIntersection> currentStepIntersections = currentStep.intersections();

    List<Pair<StepIntersection, Double>> intersectionDistances = NavigationHelper.createDistancesToIntersections(
      currentStepPoints, currentStepIntersections
    );

    assertTrue(currentStepIntersections.size() == intersectionDistances.size());
  }

  @Test
  public void createIntersectionDistanceList_emptyStepPointsReturnsEmptyList() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<Point> currentStepPoints = new ArrayList<>();
    List<StepIntersection> currentStepIntersections = currentStep.intersections();

    List<Pair<StepIntersection, Double>> intersectionDistances = NavigationHelper.createDistancesToIntersections(
      currentStepPoints, currentStepIntersections
    );

    assertTrue(intersectionDistances.isEmpty());
  }

  @Test
  public void createIntersectionDistanceList_oneStepPointReturnsEmptyList() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<Point> currentStepPoints = new ArrayList<>();
    currentStepPoints.add(Point.fromLngLat(1d, 1d));
    List<StepIntersection> currentStepIntersections = currentStep.intersections();

    List<Pair<StepIntersection, Double>> intersectionDistances = NavigationHelper.createDistancesToIntersections(
      currentStepPoints, currentStepIntersections
    );

    assertTrue(intersectionDistances.isEmpty());
  }

  @Test
  public void createIntersectionDistanceList_emptyStepIntersectionsReturnsEmptyList() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<Point> currentStepPoints = PolylineUtils.decode(currentStep.geometry(), Constants.PRECISION_6);
    List<StepIntersection> currentStepIntersections = new ArrayList<>();

    List<Pair<StepIntersection, Double>> intersectionDistances = NavigationHelper.createDistancesToIntersections(
      currentStepPoints, currentStepIntersections
    );

    assertTrue(intersectionDistances.isEmpty());
  }

  @Test
  public void findCurrentIntersection_beginningOfStepReturnsFirstIntersection() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    List<StepIntersection> intersections = stepProgress.intersections();
    List<Pair<StepIntersection, Double>> intersectionDistances = stepProgress.intersectionDistancesAlongStep();

    StepIntersection currentIntersection = NavigationHelper.findCurrentIntersection(
      intersections, intersectionDistances, 0
    );

    assertTrue(currentIntersection.equals(intersections.get(0)));
  }

  @Test
  public void findCurrentIntersection_endOfStepReturnsLastIntersection() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    List<StepIntersection> intersections = stepProgress.intersections();
    List<Pair<StepIntersection, Double>> intersectionDistances = stepProgress.intersectionDistancesAlongStep();

    StepIntersection currentIntersection = NavigationHelper.findCurrentIntersection(
      intersections, intersectionDistances, legProgress.currentStep().distance()
    );

    assertTrue(currentIntersection.equals(intersections.get(intersections.size() - 1)));
  }

  @Test
  public void findCurrentIntersection_middleOfStepReturnsCorrectIntersection() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress(100, 0, 0, 2, 0);
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    List<StepIntersection> intersections = stepProgress.intersections();
    List<Pair<StepIntersection, Double>> intersectionDistances = stepProgress.intersectionDistancesAlongStep();

    StepIntersection currentIntersection = NavigationHelper.findCurrentIntersection(
      intersections, intersectionDistances, 130
    );

    assertTrue(currentIntersection.equals(intersections.get(1)));
  }

  @Test
  public void findUpcomingIntersection_beginningOfStepReturnsSecondIntersection() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    List<StepIntersection> intersections = stepProgress.intersections();

    StepIntersection upcomingIntersection = NavigationHelper.findUpcomingIntersection(
      intersections, legProgress.upComingStep(), stepProgress.currentIntersection()
    );

    assertTrue(upcomingIntersection.equals(intersections.get(1)));
  }

  @Test
  public void findUpcomingIntersection_endOfStepReturnsUpcomingStepFirstIntersection() throws Exception {
    RouteProgress routeProgress = buildMultiLegRouteProgress();
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    List<StepIntersection> intersections = stepProgress.intersections();
    List<Pair<StepIntersection, Double>> intersectionDistances = stepProgress.intersectionDistancesAlongStep();
    StepIntersection currentIntersection = NavigationHelper.findCurrentIntersection(
      intersections, intersectionDistances, legProgress.currentStep().distance()
    );

    StepIntersection upcomingIntersection = NavigationHelper.findUpcomingIntersection(
      intersections, legProgress.upComingStep(), currentIntersection
    );

    assertEquals(legProgress.upComingStep().intersections().get(0), upcomingIntersection);
  }

  @Test
  public void findUpcomingIntersection_endOfLegReturnsNullIntersection() throws Exception {
    int stepIndex = buildMultiLegRoute().legs().get(1).steps().size() - 1;
    RouteProgress routeProgress = buildMultiLegRouteProgress(0, 0, 0, stepIndex, 1);
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    List<StepIntersection> intersections = stepProgress.intersections();
    List<Pair<StepIntersection, Double>> intersectionDistances = stepProgress.intersectionDistancesAlongStep();
    StepIntersection currentIntersection = NavigationHelper.findCurrentIntersection(
      intersections, intersectionDistances, legProgress.currentStep().distance()
    );

    StepIntersection upcomingIntersection = NavigationHelper.findUpcomingIntersection(
      intersections, legProgress.upComingStep(), currentIntersection
    );

    assertEquals(null, upcomingIntersection);
  }

  @Test
  public void createCurrentAnnotation_nullAnnotationReturnsNull() throws Exception {
    CurrentLegAnnotation currentLegAnnotation = NavigationHelper.createCurrentAnnotation(
      null, mock(RouteLeg.class), 0
    );

    assertEquals(null, currentLegAnnotation);
  }

  @Test
  public void createCurrentAnnotation_emptyDistanceArrayReturnsNull() throws Exception {
    CurrentLegAnnotation currentLegAnnotation = buildCurrentAnnotation();
    RouteLeg routeLeg = buildRouteLegWithAnnotation();

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      currentLegAnnotation, routeLeg, 0
    );

    assertEquals(null, newLegAnnotation);
  }

  @Test
  public void createCurrentAnnotation_beginningOfStep_correctAnnotationIsReturned() throws Exception {
    RouteProgress routeProgress = buildDistanceCongestionAnnotationRouteProgress(0, 0, 0, 0, 0);
    Double legDistanceRemaining = routeProgress.currentLeg().distance();

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      null, routeProgress.currentLeg(), legDistanceRemaining
    );

    assertEquals("moderate", newLegAnnotation.congestion());
  }

  @Test
  public void createCurrentAnnotation_midStep_correctAnnotationIsReturned() throws Exception {
    RouteProgress routeProgress = buildDistanceCongestionAnnotationRouteProgress(0, 0, 0, 0, 0);
    Double legDistanceRemaining = routeProgress.currentLeg().distance() / 2;

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      null, routeProgress.currentLeg(), legDistanceRemaining
    );

    assertTrue(newLegAnnotation.distanceToAnnotation() < legDistanceRemaining);
    assertEquals("heavy", newLegAnnotation.congestion());
  }

  @Test
  public void createCurrentAnnotation_usesCurrentLegAnnotationForPriorDistanceTraveled() throws Exception {
    RouteProgress routeProgress = buildDistanceCongestionAnnotationRouteProgress(0, 0, 0, 0, 0);
    Double legDistanceRemaining = routeProgress.currentLeg().distance() / 2;
    Double previousAnnotationDistance = routeProgress.currentLeg().distance() / 3;
    CurrentLegAnnotation currentLegAnnotation = CurrentLegAnnotation.builder()
      .distance(100d)
      .distanceToAnnotation(previousAnnotationDistance)
      .index(0)
      .build();

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      currentLegAnnotation, routeProgress.currentLeg(), legDistanceRemaining
    );

    assertEquals(11, newLegAnnotation.index());
  }

  private RouteProgress buildMultiLegRouteProgress(double stepDistanceRemaining, double legDistanceRemaining,
                                                   double distanceRemaining, int stepIndex, int legIndex) throws Exception {
    DirectionsRoute multiLegRoute = buildMultiLegRoute();
    return buildTestRouteProgress(multiLegRoute, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
  }

  private RouteProgress buildDistanceCongestionAnnotationRouteProgress(double stepDistanceRemaining, double legDistanceRemaining,
                                                                       double distanceRemaining, int stepIndex, int legIndex) throws Exception {
    DirectionsRoute annotatedRoute = buildDistanceCongestionAnnotationRoute();
    return buildTestRouteProgress(annotatedRoute, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
  }

  private RouteProgress buildMultiLegRouteProgress() throws Exception {
    DirectionsRoute multiLegRoute = buildMultiLegRoute();
    return buildTestRouteProgress(multiLegRoute, 1000, 1000, 1000, 0, 0);
  }

  private DirectionsRoute buildMultiLegRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(MULTI_LEG_ROUTE_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }

  private DirectionsRoute buildDistanceCongestionAnnotationRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(ANNOTATED_DISTANCE_CONGESTION_ROUTE_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }

  private CurrentLegAnnotation buildCurrentAnnotation() {
    return CurrentLegAnnotation.builder()
      .distance(54d)
      .distanceToAnnotation(100)
      .congestion("severe")
      .index(1)
      .build();
  }

  private RouteLeg buildRouteLegWithAnnotation() {
    RouteLeg routeLeg = mock(RouteLeg.class);
    LegAnnotation legAnnotation = LegAnnotation.builder()
      .distance(new ArrayList<Double>())
      .build();
    when(routeLeg.annotation()).thenReturn(legAnnotation);
    return routeLeg;
  }
}