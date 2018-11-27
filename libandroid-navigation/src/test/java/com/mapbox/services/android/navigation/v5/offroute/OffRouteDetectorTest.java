package com.mapbox.services.android.navigation.v5.offroute;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.RouteState;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OffRouteDetectorTest {

  @Test
  public void isUserOffRouteWith_returnsTrueWithRouteStateOffRoute() {
    NavigationStatus status = mock(NavigationStatus.class);
    when(status.getRouteState()).thenReturn(RouteState.OFFROUTE);
    OffRouteDetector offRouteDetector = new OffRouteDetector();

    boolean isOffRoute = offRouteDetector.isUserOffRouteWith(status);

    assertTrue(isOffRoute);
  }

  @Test
  public void isUserOffRouteWith_returnsFalseWithRouteStateOffRoute() {
    NavigationStatus status = mock(NavigationStatus.class);
    when(status.getRouteState()).thenReturn(RouteState.COMPLETE);
    OffRouteDetector offRouteDetector = new OffRouteDetector();

    boolean isOffRoute = offRouteDetector.isUserOffRouteWith(status);

    assertFalse(isOffRoute);
  }
}
