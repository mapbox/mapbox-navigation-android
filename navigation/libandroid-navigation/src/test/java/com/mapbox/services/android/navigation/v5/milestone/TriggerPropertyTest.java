package com.mapbox.services.android.navigation.v5.milestone;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class TriggerPropertyTest extends BaseTest {

  // Fixtures
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private RouteProgress routeProgress;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.getRoutes().get(0);

    routeProgress = RouteProgress.builder()
      .directionsRoute(route)
      .distanceRemaining(route.getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legIndex(0)
      .stepIndex(1)
      .location(mock(Location.class))
      .build();
  }

  @Test
  public void stepDurationRemainingProperty_onlyPassesValidationWhenEqual() {
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
  public void stepDistanceRemainingProperty_onlyPassesValidationWhenEqual() {
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
  public void stepDistanceTotalProperty_onlyPassesValidationWhenEqual() {
    double stepDistanceTotal = routeProgress.currentLegProgress().currentStep().getDistance();

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
  public void stepDurationTotalProperty_onlyPassesValidationWhenEqual() {
    double stepDurationTotal = routeProgress.currentLegProgress().currentStep().getDuration();

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
  public void stepIndexProperty_onlyPassesValidationWhenEqual() {
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
}
