package com.mapbox.services.android.navigation.v5.offroute;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouteState;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class OffRouteDetectorTest {

  @Test
  public void isUserOffRouteWith_returnsTrueWithRouteStateOffRoute() {
    Navigator navigator = mock(Navigator.class);
    NavigationStatus status = mock(NavigationStatus.class);
    when(status.getRouteState()).thenReturn(RouteState.OFFROUTE);
    when(navigator.getStatus(any(Date.class))).thenReturn(status);
    OffRouteDetector offRouteDetector = new OffRouteDetector(navigator);

    boolean isOffRoute = offRouteDetector.isUserOffRouteWith(new Date());

    assertTrue(isOffRoute);
  }

  @Test
  public void isUserOffRouteWith_returnsFalseWithRouteStateOffRoute() {
    Navigator navigator = mock(Navigator.class);
    NavigationStatus status = mock(NavigationStatus.class);
    when(status.getRouteState()).thenReturn(RouteState.COMPLETE);
    when(navigator.getStatus(any(Date.class))).thenReturn(status);
    OffRouteDetector offRouteDetector = new OffRouteDetector(navigator);

    boolean isOffRoute = offRouteDetector.isUserOffRouteWith(new Date());

    assertFalse(isOffRoute);
  }
}
