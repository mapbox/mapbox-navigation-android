package com.mapbox.services.android.navigation.v5.milestone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class StepMilestoneTest extends BaseTest {

  private static final String ROUTE_FIXTURE = "directions_v5_precision_6.json";

  @Test
  public void sanity() throws Exception {
    RouteProgress routeProgress = buildStepMilestoneRouteProgress();
    Milestone milestone = new StepMilestone.Builder()
      .setTrigger(
        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
      )
      .setIdentifier(101)
      .build();

    assertNotNull(milestone);
    assertTrue(milestone.isOccurring(routeProgress, routeProgress));
  }

  @Test
  public void getIdentifier_doesEqualSetValue() {
    Milestone milestone = new StepMilestone.Builder()
      .setIdentifier(101)
      .build();

    assertEquals(101, milestone.getIdentifier());
  }

  private RouteProgress buildStepMilestoneRouteProgress() throws Exception {
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
