package com.mapbox.services.android.navigation.v5.milestone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class TriggerTest extends BaseTest {

  private static final String ROUTE_FIXTURE = "directions_v5_precision_6.json";

  @Test
  public void triggerAll_noStatementsProvidedResultsInTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.all())
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void triggerAll_validatesAllStatements() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerAll_oneConditionsFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerAny_noConditionsAreTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerAny_validatesAllStatementsTillOnesTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerAny_oneConditionsTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerNone_noConditionsAreTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerNone_validatesAllStatementsTillOnesTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
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
  public void triggerNone_onoConditionsTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(Trigger.none(
        Trigger.gt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 100d),
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      ))
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void greaterThan_validatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void greaterThan_validatesToFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void greaterThanEqual_validatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void greaterThanEqual_equalStillValidatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().distance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void greaterThanEqual_validatesToFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void lessThan_validatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void lessThan_validatesToFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void lessThanEqual_validatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 10000d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void lessThanEqual_equalStillValidatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().distance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void lessThanEqual_validatesToFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.lte(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void equal_validatesToFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.eq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void equal_validatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.eq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().distance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  @Test
  public void notEqual_validatesToFalse() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.neq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
          routeProgress.currentLegProgress().currentStep().distance())
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertFalse(result);
  }

  @Test
  public void notEqual_validatesToTrue() throws Exception {
    RouteProgress routeProgress = buildTriggerRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.neq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .build();

    boolean result = milestone.isOccurring(routeProgress, routeProgress);

    Assert.assertTrue(result);
  }

  private RouteProgress buildTriggerRouteProgress() throws Exception {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(ROUTE_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.routes().get(0);
    int stepDistanceRemaining = (int) route.legs().get(0).steps().get(0).distance();
    int legDistanceRemaining = route.legs().get(0).distance().intValue();
    int routeDistance = route.distance().intValue();
    return buildTestRouteProgress(route, stepDistanceRemaining, legDistanceRemaining,
      routeDistance, 1, 0);
  }
}
