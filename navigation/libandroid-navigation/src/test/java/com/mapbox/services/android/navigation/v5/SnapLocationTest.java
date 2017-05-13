package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;

public class SnapLocationTest extends BaseTest {

  @Test
  public void sanityTest() {
    SnapLocation snapLocation = new SnapLocation(Mockito.mock(Location.class), Mockito.mock(RouteProgress.class),
      new MapboxNavigationOptions());
    assertNotNull("should not be null", snapLocation);
  }

}