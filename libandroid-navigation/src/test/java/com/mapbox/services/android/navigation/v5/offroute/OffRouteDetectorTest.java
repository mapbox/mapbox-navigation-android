package com.mapbox.services.android.navigation.v5.offroute;

import static junit.framework.Assert.assertNotNull;

import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

public class OffRouteDetectorTest extends BaseTest {

  @Test
  public void sanity() throws Exception {
    OffRoute offRoute = new OffRouteDetector();
    assertNotNull(offRoute);
  }

  @Test
  public void isUserOffRoute_falseWhenMovingTowardsManeuver() throws Exception {
    

  }
}
