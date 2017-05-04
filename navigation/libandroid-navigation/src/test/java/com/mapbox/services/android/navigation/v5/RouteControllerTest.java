package com.mapbox.services.android.navigation.v5;

import junit.framework.Assert;

import org.junit.Test;

public class RouteControllerTest {

  @Test
  public void departingUserManeuverZoneRadius() {

    int alertLevel = RouteController.isUserDeparting(NavigationConstants.NONE_ALERT_LEVEL, 15, 10);
    Assert.assertEquals(NavigationConstants.DEPART_ALERT_LEVEL, alertLevel);
    alertLevel = RouteController.isUserDeparting(NavigationConstants.NONE_ALERT_LEVEL, 5, 10);
    Assert.assertEquals(NavigationConstants.HIGH_ALERT_LEVEL, alertLevel);
  }

  @Test
  public void isUserArriving() throws Exception {


  }
}
