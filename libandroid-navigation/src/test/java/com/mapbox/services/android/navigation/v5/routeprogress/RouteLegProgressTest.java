package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class RouteLegProgressTest extends BaseTest {

  @Test
  public void sanityTest() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();

    assertNotNull(routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() throws Exception {
    int stepIndex = 5;
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    List<LegStep> steps = routeProgress.currentLeg().steps();

    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();
    int upComingStepIndex = steps.indexOf(upComingStep);

    assertEquals(stepIndex + 1, upComingStepIndex);
  }

  @Test
  public void upComingStep_returnsNull() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);

    LegStep upComingStep = findUpcomingStep(routeProgress, firstLeg);

    assertNull(upComingStep);
  }

  @Test
  public void currentStep_returnsCurrentStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);

    routeProgress = routeProgress.toBuilder().stepIndex(5).build();

    assertEquals(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(6).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);

    routeProgress = routeProgress.toBuilder().stepIndex(5).build();

    assertEquals(
      firstLeg.steps().get(4).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();

    routeProgress = routeProgress.toBuilder().stepIndex(3).build();

    assertEquals(3, routeProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(0.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    double stepSegmentsInMeters = 5000;
    List<Float> fractionsRemaining = new ArrayList<>();
    List<Float> routeProgressFractionsTraveled = new ArrayList<>();

    for (double i = 0; i < firstLeg.distance(); i += stepSegmentsInMeters) {
      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled() / firstLeg.distance());
      fractionsRemaining.add(fractionRemaining);
      routeProgressFractionsTraveled.add(routeProgress.currentLegProgress().fractionTraveled());
    }

    assertTrue(fractionsRemaining.equals(routeProgressFractionsTraveled));
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfLeg() throws Exception {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(1.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceRemaining_equalsLegDistanceAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);

    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceRemaining(),
      BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfLeg() throws Exception {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsLegDistanceAtEndOfLeg() throws Exception {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);

    Double firstLegDistance = firstLeg.distance();
    double distanceTraveled = routeProgress.currentLegProgress().distanceTraveled();

    assertEquals(firstLegDistance, distanceTraveled, BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();
    DirectionsRoute route = routeProgress.directionsRoute();
    RouteLeg firstLeg = route.legs().get(0);

    Double firstLegDuration = firstLeg.duration();
    double currentLegDurationRemaining = routeProgress.currentLegProgress().durationRemaining();

    assertEquals(firstLegDuration, currentLegDurationRemaining, BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() throws Exception {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    int stepIndex = 5;
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder().stepIndex(stepIndex).build();
    List<LegStep> steps = routeProgress.directionsRoute().legs().get(0).steps();

    LegStep followOnStep = routeProgress.currentLegProgress().followOnStep();
    int followOnIndex = steps.indexOf(followOnStep);

    assertEquals(stepIndex + 2, followOnIndex);
  }

  @Test
  public void followOnStep_returnsNull() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int lastStepIndex = firstLeg.steps().size() - 1;

    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();

    assertNull(routeProgress.currentLegProgress().followOnStep());
  }

  private RouteProgress buildBeginningOfLegRouteProgress() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    double stepDistanceRemaining = route.legs().get(0).steps().get(0).distance();
    double legDistanceRemaining = route.legs().get(0).distance();
    double routeDistance = route.distance();
    return buildTestRouteProgress(route, stepDistanceRemaining, legDistanceRemaining,
      routeDistance, 0, 0);
  }

  private RouteProgress buildEndOfLegRouteProgress() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int lastStepIndex = firstLeg.steps().size() - 1;
    return buildTestRouteProgress(route, 0, 0, 0, lastStepIndex, 0);
  }

  private LegStep findUpcomingStep(RouteProgress routeProgress, RouteLeg firstLeg) {
    int lastStepIndex = firstLeg.steps().size() - 1;
    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();
    return routeProgress.currentLegProgress().upComingStep();
  }
}
