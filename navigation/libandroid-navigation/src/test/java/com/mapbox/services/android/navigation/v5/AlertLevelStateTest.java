package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;

public class AlertLevelStateTest extends BaseTest {

  @Test
  public void sanityTest() {
    AlertLevelState alertLevelState = new AlertLevelState(Mockito.mock(Location.class),
      Mockito.mock(RouteProgress.class), 0 ,0, new MapboxNavigationOptions());
    assertNotNull("should not be null", alertLevelState);
  }
}