package com.mapbox.services.android.navigation.v5.milestone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class StepMilestoneTest extends BaseTest {

  // Fixtures
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private RouteProgress routeProgress;

  @Before
  public void setup() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.routes().get(0);

    routeProgress = RouteProgress.builder()
      .directionsRoute(route)
      .distanceRemaining(route.distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legIndex(0)
      .stepIndex(1)
      .build();
  }

  @Test
  public void sanity() {
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .setIdentifier(101)
      .build();

    Assert.assertNotNull(milestone);
    Assert.assertTrue(milestone.isOccurring(routeProgress, routeProgress));
  }

  @Test
  public void getIdentifier_doesEqualSetValue() {
    Milestone milestone = new StepMilestone.Builder()
      .setIdentifier(101)
      .build();

    Assert.assertEquals(101, milestone.getIdentifier());
  }
}
