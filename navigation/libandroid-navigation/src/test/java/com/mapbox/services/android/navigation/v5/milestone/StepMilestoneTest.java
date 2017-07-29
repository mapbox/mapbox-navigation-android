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
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class StepMilestoneTest extends BaseTest {
//
//  // Fixtures
//  private static final String PRECISION_6 = "directions_v5_precision_6.json";
//
//  private RouteProgress routeProgress;
//  private RouteProgress previousRouteProgress;
//
//  @Before
//  public void setup() {
//    Gson gson = new Gson();
//    String body = readPath(PRECISION_6);
//    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
//    DirectionsRoute route = response.getRoutes().get(0);
//
//    previousRouteProgress = RouteProgress.create(route, Mockito.mock(Location.class), 0, 1);
//    routeProgress = RouteProgress.create(route, Mockito.mock(Location.class), 0, 1);
//  }
//
//  @Test
//  public void sanity() {
//    Milestone milestone = new StepMilestone.Builder()
//      .setTrigger(
//        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
//      )
//      .setIdentifier(101)
//      .build();
//
//    Assert.assertNotNull(milestone);
//    Assert.assertTrue(milestone.isOccurring(previousRouteProgress, routeProgress));
//  }
//
//  @Test
//  public void getIdentifier_doesEqualSetValue() {
//    Milestone milestone = new StepMilestone.Builder()
//      .setIdentifier(101)
//      .build();
//
//    Assert.assertEquals(101, milestone.getIdentifier());
//  }
}
