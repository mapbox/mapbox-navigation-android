package com.mapbox.services.android.navigation.v5.milestone;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class TriggerTest extends BaseTest {

  // Fixtures
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private RouteProgress routeProgress;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.getRoutes().get(0);
    Location location = new Location("test");
    List<Position> coords = PolylineUtils.decode(route.getLegs().get(0).getSteps().get(1).getGeometry(),
      Constants.PRECISION_6);
    location.setLatitude(coords.get(0).getLatitude());
    location.setLongitude(coords.get(0).getLongitude());
    routeProgress = RouteProgress.builder()
      .directionsRoute(route)
      .distanceRemaining(route.getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legIndex(0)
      .stepIndex(1)
      .build();
  }

  /*
   * Compound statement test
   */

  @Test
  public void triggerAll_noStatementsProvidedResultsInTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.all())
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void triggerAll_validatesAllStatements() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.all(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d),
        Trigger.eq(TriggerProperty.STEP_INDEX, 1)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void triggerAll_oneConditionsFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.all(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d),
        Trigger.eq(TriggerProperty.NEW_STEP, TriggerProperty.FALSE)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void triggerAny_noConditionsAreTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.any(
        Trigger.gt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 200d),
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void triggerAny_validatesAllStatementsTillOnesTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.any(
        Trigger.eq(TriggerProperty.STEP_INDEX, 1),
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d),
        Trigger.eq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void triggerAny_oneConditionsTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.any(
        Trigger.gt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 100d),
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }


  @Test
  public void triggerNone_noConditionsAreTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.none(
        Trigger.gt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 200d),
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void triggerNone_validatesAllStatementsTillOnesTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.none(
        Trigger.neq(TriggerProperty.STEP_INDEX, 1),
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void triggerNone_onoConditionsTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.none(
        Trigger.gt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 100d),
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  /*
   * Simple statement test
   */

  @Test
  public void greaterThan_validatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void greaterThan_validatesToFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void greaterThanEqual_validatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void greaterThanEqual_equalStillValidatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().getDistance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void greaterThanEqual_validatesToFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void lessThan_validatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void lessThan_validatesToFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void lessThanEqual_validatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void lessThanEqual_equalStillValidatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().getDistance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void lessThanEqual_validatesToFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void equal_validatesToFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.eq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void equal_validatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.eq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().getDistance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }

  @Test
  public void notEqual_validatesToFalse() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.neq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().getDistance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertFalse(result);
  }

  @Test
  public void notEqual_validatesToTrue() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.neq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);
    Assert.assertTrue(result);
  }
}
