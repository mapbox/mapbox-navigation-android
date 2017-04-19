package com.mapbox.services.android.navigation.v5;

import com.google.gson.Gson;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RouteProgressTest extends BaseTest {

  private static final int NONE_ALERT_LEVEL = 0;
  private static final int LOW_ALERT_LEVEL = 2;
  private static final String NAVIGATION_FIXTURE = "navigation.json";

  private DirectionsRoute route;
  private RouteProgress routeProgress;

  @Before
  public void setUp() throws IOException {
    Gson gson = new Gson();
    String body = readPath(NAVIGATION_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);

    Position userSnappedPosition = Position.fromCoordinates(-122.413165, 37.795042);
    routeProgress = new RouteProgress(route, userSnappedPosition, 0, 0, NONE_ALERT_LEVEL);
  }

  @Test
  public void routeProgressTest() throws IOException {
    assertEquals(routeProgress.getCurrentLeg(), route.getLegs().get(0));
    assertEquals(routeProgress.getAlertUserLevel(), NONE_ALERT_LEVEL);
    assertEquals(routeProgress.getLegIndex(), 0);
    assertEquals(routeProgress.getFractionTraveled(), 0, DELTA);
    assertEquals(routeProgress.getDistanceRemaining(), 4317.5, 0.1);
    assertEquals(routeProgress.getDistanceTraveled(), 0, DELTA);
    assertEquals(routeProgress.getDurationRemaining(), 916, DELTA);
  }

  @Test
  public void routeLegProgressTest() {
    assertEquals(routeProgress.getCurrentLegProgress().getUpComingStep(), route.getLegs().get(0).getSteps().get(1));
    assertEquals(routeProgress.getCurrentLegProgress().getDistanceRemaining(), 4317.5, 0.1);
    assertEquals(routeProgress.getCurrentLegProgress().getDurationRemaining(), 916);
    assertEquals(routeProgress.getCurrentLegProgress().getFractionTraveled(), 0, DELTA);
    assertEquals(routeProgress.getCurrentLegProgress().getCurrentStep(), route.getLegs().get(0).getSteps().get(0));
    assertEquals(routeProgress.getCurrentLegProgress().getDistanceTraveled(), 0, DELTA);
    assertEquals(routeProgress.getCurrentLegProgress().getPreviousStep(), null);
    assertEquals(routeProgress.getCurrentLegProgress().getStepIndex(), 0);
  }

  @Test
  public void routeStepProgressTest() {
    assertEquals(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining(), 279.8, 0.1);
    assertEquals(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getFractionTraveled(), 0, DELTA);
    assertEquals(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceTraveled(), 0, DELTA);
    assertEquals(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDurationRemaining(), 69, DELTA);
  }

  @Test
  public void nextRouteStepProgressTest() {
    Position userNextStepSnappedPosition = route.getLegs().get(0).getSteps().get(1).getManeuver().asPosition();
    RouteProgress nextRouteProgress = new RouteProgress(route, userNextStepSnappedPosition, 0, 1, LOW_ALERT_LEVEL);

    assertEquals(nextRouteProgress.getCurrentLeg(), route.getLegs().get(0));
    assertEquals(nextRouteProgress.getAlertUserLevel(), LOW_ALERT_LEVEL);
    assertEquals(nextRouteProgress.getLegIndex(), 0);
    assertEquals(nextRouteProgress.getDistanceRemaining(), 4037.6, 0.1);
    assertEquals(nextRouteProgress.getDistanceTraveled(), 279.8, 0.1);
    assertEquals(nextRouteProgress.getDurationRemaining(), 856, DELTA);
    assertEquals(nextRouteProgress.getFractionTraveled(), 0.0648, 0.0001);
  }
}
