package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

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
    RouteProgress routeProgress = buildDefaultTestRouteProgress();

    routeProgress = routeProgress.toBuilder().stepIndex(5).build();

    assertTrue(routeProgress.currentLegProgress().upComingStep().geometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
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

    for (double i = 0; i < firstLeg.distance(); i += stepSegmentsInMeters) {
      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled() / firstLeg.distance());

      assertEquals(fractionRemaining, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
    }
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

    assertEquals(3535.2, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() throws Exception {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();

    routeProgress = routeProgress.toBuilder().stepIndex(5).build();

    assertTrue(routeProgress.currentLegProgress().followOnStep().geometry()
      .startsWith("un`ffAz_dogFz`Aq^hF{B|GaB|HcAxKKlIp@lOzC|Dh@hKtAzFh@`FDvHy@bG{AjKaEfF"));
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
