package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public class RouteControllerTest extends BaseTest {

  private DirectionsRoute route;

  @Before
  public void setUp() throws IOException {
    Gson gson = new Gson();
    String body = readPath("directions_v5.json");
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void isUserDeparting_userOutsideManeuverRadius() throws Exception {
    int alertLevel = RouteController.isUserDeparting(NavigationConstants.NONE_ALERT_LEVEL, 15, 10);
    Assert.assertEquals(NavigationConstants.DEPART_ALERT_LEVEL, alertLevel);
  }

  @Test
  public void isUserDeparting_userInsideManeuverRadius() throws Exception {
    int alertLevel = RouteController.isUserDeparting(NavigationConstants.NONE_ALERT_LEVEL, 5, 10);
    Assert.assertEquals(NavigationConstants.HIGH_ALERT_LEVEL, alertLevel);
  }

  @Test
  public void isUserArriving_alertLevelShouldBeArrive() throws Exception {
    RouteProgress routeProgress
      = new RouteProgress(route, Mockito.mock(Location.class), 0, 11, NavigationConstants.MEDIUM_ALERT_LEVEL);
    int alertLevel = RouteController.isUserArriving(NavigationConstants.MEDIUM_ALERT_LEVEL, routeProgress);
    Assert.assertEquals(NavigationConstants.ARRIVE_ALERT_LEVEL, alertLevel);
  }

  @Test
  public void isUserArriving_alertLevelShouldBeMedium() throws Exception {
    RouteProgress routeProgress
      = new RouteProgress(route, Mockito.mock(Location.class), 0, 10, NavigationConstants.MEDIUM_ALERT_LEVEL);
    int alertLevel = RouteController.isUserArriving(NavigationConstants.MEDIUM_ALERT_LEVEL, routeProgress);
    Assert.assertEquals(NavigationConstants.MEDIUM_ALERT_LEVEL, alertLevel);
  }

  // TODO improve increaseIndex for testing
  @Test
  public void increaseIndex_stepIndexShouldIncreaseByOne() throws Exception {
    MapboxNavigationOptions options = new MapboxNavigationOptions();
    RouteController routeController = new RouteController(options);
    RouteProgress routeProgress
      = new RouteProgress(route, Mockito.mock(Location.class), 0, 0, NavigationConstants.HIGH_ALERT_LEVEL);
    routeController.increaseIndex(routeProgress);
    Assert.assertEquals(1, routeController.getCurrentStepIndex());
  }

  @Test
  public void nextStepAlert_shouldReturnLowAlert() throws Exception {
    MapboxNavigationOptions options = new MapboxNavigationOptions();
    RouteController routeController = new RouteController(options);
    RouteProgress routeProgress
      = new RouteProgress(route, Mockito.mock(Location.class), 0, 1, NavigationConstants.HIGH_ALERT_LEVEL);
    int alertLevel = routeController.nextStepAlert(mock(Location.class), routeProgress);
    Assert.assertEquals(NavigationConstants.LOW_ALERT_LEVEL, alertLevel);
  }
}
