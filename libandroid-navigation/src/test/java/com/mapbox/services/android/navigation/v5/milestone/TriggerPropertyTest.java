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
public class TriggerPropertyTest extends BaseTest {

  private static final String ROUTE_FIXTURE = "directions_v5_precision_6.json";

  @Test
  public void stepDurationRemainingProperty_onlyPassesValidationWhenEqual() throws Exception {
    RouteProgress routeProgress = buildTestRouteProgressForTrigger();
    double stepDuration = routeProgress.currentLegProgress().currentStepProgress().durationRemaining();

    for (int i = 10; i > 0; i--) {
      Milestone milestone = new StepMilestone.Builder()
        .setTrigger(
          Trigger.eq(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, (stepDuration / i))
        ).build();

      boolean result = milestone.isOccurring(routeProgress, routeProgress);
      if ((stepDuration / i) == stepDuration) {
        Assert.assertTrue(result);
      } else {
        Assert.assertFalse(result);
      }
    }
  }

  @Test
  public void stepDistanceRemainingProperty_onlyPassesValidationWhenEqual() throws Exception {
    RouteProgress routeProgress = buildTestRouteProgressForTrigger();
    double stepDistance = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    for (int i = 10; i > 0; i--) {
      Milestone milestone = new StepMilestone.Builder()
        .setTrigger(
          Trigger.eq(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, (stepDistance / i))
        ).build();

      boolean result = milestone.isOccurring(routeProgress, routeProgress);
      if ((stepDistance / i) == stepDistance) {
        Assert.assertTrue(result);
      } else {
        Assert.assertFalse(result);
      }
    }
  }

  @Test
  public void stepDistanceTotalProperty_onlyPassesValidationWhenEqual() throws Exception {
    RouteProgress routeProgress = buildTestRouteProgressForTrigger();
    double stepDistanceTotal = routeProgress.currentLegProgress().currentStep().distance();

    for (int i = 10; i > 0; i--) {
      Milestone milestone = new StepMilestone.Builder()
        .setTrigger(
          Trigger.eq(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, (stepDistanceTotal / i))
        ).build();

      boolean result = milestone.isOccurring(routeProgress, routeProgress);
      if ((stepDistanceTotal / i) == stepDistanceTotal) {
        Assert.assertTrue(result);
      } else {
        Assert.assertFalse(result);
      }
    }
  }

  @Test
  public void stepDurationTotalProperty_onlyPassesValidationWhenEqual() throws Exception {
    RouteProgress routeProgress = buildTestRouteProgressForTrigger();
    double stepDurationTotal = routeProgress.currentLegProgress().currentStep().duration();

    for (int i = 10; i > 0; i--) {
      Milestone milestone = new StepMilestone.Builder()
        .setTrigger(
          Trigger.eq(TriggerProperty.STEP_DURATION_TOTAL_SECONDS, (stepDurationTotal / i))
        ).build();

      boolean result = milestone.isOccurring(routeProgress, routeProgress);
      if ((stepDurationTotal / i) == stepDurationTotal) {
        Assert.assertTrue(result);
      } else {
        Assert.assertFalse(result);
      }
    }
  }

  @Test
  public void stepIndexProperty_onlyPassesValidationWhenEqual() throws Exception {
    RouteProgress routeProgress = buildTestRouteProgressForTrigger();
    int stepIndex = routeProgress.currentLegProgress().stepIndex();

    for (int i = 10; i > 0; i--) {
      Milestone milestone = new StepMilestone.Builder()
        .setTrigger(
          Trigger.eq(TriggerProperty.STEP_INDEX, Math.abs(stepIndex - i))
        ).build();

      boolean result = milestone.isOccurring(routeProgress, routeProgress);
      if (Math.abs(stepIndex - i) == stepIndex) {
        Assert.assertTrue(result);
      } else {
        Assert.assertFalse(result);
      }
    }
  }

  private RouteProgress buildTestRouteProgressForTrigger() throws Exception {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(ROUTE_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);

    DirectionsRoute route = response.routes().get(0);
    double distanceRemaining = route.distance();
    double legDistanceRemaining = route.legs().get(0).distance();
    double stepDistanceRemaining = route.legs().get(0).steps().get(0).distance();
    return buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, 1, 0);
  }
}
