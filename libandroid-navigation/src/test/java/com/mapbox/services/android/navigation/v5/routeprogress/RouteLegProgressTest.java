package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class RouteLegProgressTest extends BaseTest {

  private DirectionsRoute route;
  private RouteProgress routeProgress;
  private RouteLeg firstLeg;

  @Before
  public void setup() throws Exception {
    route = buildDirectionsRoute();
    firstLeg = route.legs().get(0);
    routeProgress = buildDefaultRouteProgress();
  }

  @Test
  public void sanityTest() {
    assertNotNull("should not be null", routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertTrue(routeProgress.currentLegProgress().upComingStep().geometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
  }

  @Test
  public void upComingStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();
    assertNull(routeProgress.currentLegProgress().upComingStep());
  }

  @Test
  public void currentStep_returnsCurrentStep() {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertEquals(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(6).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertEquals(
      firstLeg.steps().get(4).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() {
    routeProgress = routeProgress.toBuilder().stepIndex(3).build();

    assertEquals(3, routeProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(0.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    double stepSegments = 5000; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstLeg.distance(); i += stepSegments) {
      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled()
        / firstLeg.distance());
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

    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(3535.2, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() throws Exception {
    routeProgress = buildEndOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();

    assertTrue(routeProgress.currentLegProgress().followOnStep().geometry()
      .startsWith("un`ffAz_dogFz`Aq^hF{B|GaB|HcAxKKlIp@lOzC|Dh@hKtAzFh@`FDvHy@bG{AjKaEfF"));
  }

  @Test
  public void followOnStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();

    assertNull(routeProgress.currentLegProgress().followOnStep());
  }

  private RouteProgress buildBeginningOfLegRouteProgress() throws Exception {
    double stepDistanceRemaining = route.legs().get(0).steps().get(0).distance();
    double legDistanceRemaining = route.legs().get(0).distance();
    double routeDistance = route.distance();
    return buildRouteProgress(route, stepDistanceRemaining, legDistanceRemaining,
      routeDistance, 0, 0);
  }

  private RouteProgress buildEndOfLegRouteProgress() throws Exception {
    int lastStepIndex = firstLeg.steps().size() - 1;
    return buildRouteProgress(route, 0, 0,
      0, lastStepIndex, 0);
  }
}
