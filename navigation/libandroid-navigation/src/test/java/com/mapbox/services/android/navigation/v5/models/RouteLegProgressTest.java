package com.mapbox.services.android.navigation.v5.models;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RouteLegProgressTest extends BaseTest {

  // Fixtures
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private DirectionsRoute route;
  private RouteLeg firstLeg;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
    firstLeg = route.getLegs().get(0);
  }

  @Test
  public void sanityTest() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 0, Mockito.mock(Position.class));
    Assert.assertNotNull("should not be null", routeLegProgress);
  }

  @Test
  public void getUpComingStep_returnsNextStepInLeg() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 5, firstLeg.getSteps().get(4).getManeuver().asPosition());

    // TODO replace with equalsTo once https://github.com/mapbox/mapbox-java/pull/450 merged
    Assert.assertTrue(routeLegProgress.getUpComingStep().getGeometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));

  }

  @Test
  public void getUpComingStep_returnsNull() {
    int lastStepIndex = firstLeg.getSteps().size() - 1;
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, lastStepIndex,
      firstLeg.getSteps().get(lastStepIndex - 2).getManeuver().asPosition());

    Assert.assertNull(routeLegProgress.getUpComingStep());
  }

  @Test
  public void getCurrentStep_returnsCurrentStep() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 5, firstLeg.getSteps().get(4).getManeuver().asPosition());

    // TODO replace with equalsTo once https://github.com/mapbox/mapbox-java/pull/450 merged
    Assert.assertEquals(
      firstLeg.getSteps().get(5).getGeometry(), routeLegProgress.getCurrentStep().getGeometry()
    );
    Assert.assertNotSame(
      firstLeg.getSteps().get(6).getGeometry(), routeLegProgress.getCurrentStep().getGeometry()
    );
  }

  @Test
  public void getPreviousStep_returnsPreviousStep() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 5, firstLeg.getSteps().get(4).getManeuver().asPosition());

    // TODO replace with equalsTo once https://github.com/mapbox/mapbox-java/pull/450 merged
    Assert.assertEquals(
      firstLeg.getSteps().get(4).getGeometry(), routeLegProgress.getPreviousStep().getGeometry()
    );
    Assert.assertNotSame(
      firstLeg.getSteps().get(5).getGeometry(), routeLegProgress.getPreviousStep().getGeometry()
    );
  }

  @Test
  public void getStepIndex_returnsCurrentStepIndex() {
    RouteLegProgress routeLegProgress = new RouteLegProgress(firstLeg, 3,
      firstLeg.getSteps().get(4).getManeuver().asPosition());

    Assert.assertEquals(3, routeLegProgress.getStepIndex(), BaseTest.DELTA);
  }

  @Test
  public void getFractionTraveled_equalsZeroAtBeginning() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 0, firstLeg.getSteps().get(0).getManeuver().asPosition());

    Assert.assertEquals(0, routeLegProgress.getFractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getFractionTraveled_equalsCorrectValueAtIntervals() {
    double legDistance = 0;
    for (LegStep step : firstLeg.getSteps()) {
      LineString lineString
        = LineString.fromPolyline(step.getGeometry(), Constants.PRECISION_6);
      legDistance += TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);
    }

    double stepSegments = 5000; // meters

    // Chop the line in small pieces
    LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6);
    for (double i = 0; i < legDistance; i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      RouteLegProgress routeLegProgress = new RouteLegProgress(firstLeg, 0, position);
      float fractionRemaining = (float) (routeLegProgress.getDistanceTraveled() / legDistance);
      Assert.assertEquals(fractionRemaining, routeLegProgress.getFractionTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void getFractionTraveled_equalsOneAtEndOfLeg() {
    RouteLegProgress routeLegProgress = new RouteLegProgress(firstLeg, firstLeg.getSteps().size() - 1,
      firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition());

    Assert.assertEquals(1.0, routeLegProgress.getFractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceRemaining_equalsLegDistanceAtBeginning() {
    double legDistance = 0;
    for (LegStep step : firstLeg.getSteps()) {
      LineString lineString
        = LineString.fromPolyline(step.getGeometry(), Constants.PRECISION_6);
      legDistance += TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);
    }

    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 0, firstLeg.getSteps().get(0).getManeuver().asPosition());

    Assert.assertEquals(legDistance, routeLegProgress.getDistanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceRemaining_equalsZeroAtEndOfLeg() {
    RouteLegProgress routeLegProgress = new RouteLegProgress(firstLeg, firstLeg.getSteps().size() - 1,
      firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition());

    Assert.assertEquals(0, routeLegProgress.getDistanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsZeroAtBeginning() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 0, firstLeg.getSteps().get(0).getManeuver().asPosition());
    Assert.assertEquals(0, routeLegProgress.getDistanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsLegDistanceAtEndOfLeg() {
    double traveledLegDistance = 0;
    for (int i = 0; i < 3; i++) {
      LineString traveledLineString
        = LineString.fromPolyline(firstLeg.getSteps().get(i).getGeometry(), Constants.PRECISION_6);
      traveledLegDistance += TurfMeasurement.lineDistance(traveledLineString, TurfConstants.UNIT_METERS);
    }

    RouteLegProgress routeLegProgress = new RouteLegProgress(firstLeg, 4,
      firstLeg.getSteps().get(3).getManeuver().asPosition());

    Assert.assertEquals(traveledLegDistance, routeLegProgress.getDistanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() {
    RouteLegProgress routeLegProgress
      = new RouteLegProgress(firstLeg, 0, firstLeg.getSteps().get(0).getManeuver().asPosition());

    Assert.assertEquals(3535.2, routeLegProgress.getDurationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() {
    RouteLegProgress routeLegProgress = new RouteLegProgress(firstLeg, firstLeg.getSteps().size() - 1,
      firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition());

    Assert.assertEquals(0, routeLegProgress.getDurationRemaining(), BaseTest.DELTA);
  }
}
