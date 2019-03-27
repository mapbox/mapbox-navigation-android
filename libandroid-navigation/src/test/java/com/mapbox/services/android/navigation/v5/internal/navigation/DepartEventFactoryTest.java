package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation;
import com.mapbox.services.android.navigation.v5.navigation.metrics.SessionState;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DepartEventFactoryTest {

  @Test
  public void send_nullStartTimestampAndValidDistanceSendsEvent() {
    DepartEventHandler handler = mock(DepartEventHandler.class);
    SessionState sessionState = SessionState.builder().build();
    MetricsRouteProgress routeProgress = mock(MetricsRouteProgress.class);
    when(routeProgress.getLegIndex()).thenReturn(0);
    when(routeProgress.getDistanceTraveled()).thenReturn(100);
    MetricsLocation location = mock(MetricsLocation.class);
    DepartEventFactory factory = new DepartEventFactory(handler);

    SessionState sentState = factory.send(sessionState, routeProgress, location);

    assertNotNull(sentState.startTimestamp());
  }

  @Test
  public void send_nullStartTimestampAndInvalidDistanceDoesNotSendEvent() {
    DepartEventHandler handler = mock(DepartEventHandler.class);
    SessionState sessionState = SessionState.builder().build();
    MetricsRouteProgress routeProgress = mock(MetricsRouteProgress.class);
    when(routeProgress.getLegIndex()).thenReturn(0);
    when(routeProgress.getDistanceTraveled()).thenReturn(0);
    MetricsLocation location = mock(MetricsLocation.class);
    DepartEventFactory factory = new DepartEventFactory(handler);

    SessionState sentState = factory.send(sessionState, routeProgress, location);

    assertNull(sentState.startTimestamp());
  }

  @Test
  public void send_nonNullStartTimestampAndValidDistanceDoesNotSendEvent() {
    DepartEventHandler handler = mock(DepartEventHandler.class);
    SessionState sessionState = SessionState.builder().build();
    MetricsRouteProgress routeProgress = mock(MetricsRouteProgress.class);
    when(routeProgress.getLegIndex()).thenReturn(0);
    when(routeProgress.getDistanceTraveled()).thenReturn(100);
    MetricsLocation location = mock(MetricsLocation.class);
    DepartEventFactory factory = new DepartEventFactory(handler);

    SessionState updatedState = factory.send(sessionState, routeProgress, location);
    factory.send(updatedState, routeProgress, location);

    verify(handler, times(1)).send(eq(updatedState), eq(routeProgress), eq(location));
  }

  @Test
  public void send_resetAllowsNewEventToBeSent() {
    DepartEventHandler handler = mock(DepartEventHandler.class);
    SessionState sessionState = SessionState.builder().build();
    MetricsRouteProgress routeProgress = mock(MetricsRouteProgress.class);
    when(routeProgress.getLegIndex()).thenReturn(0);
    when(routeProgress.getDistanceTraveled()).thenReturn(100);
    MetricsLocation location = mock(MetricsLocation.class);
    DepartEventFactory factory = new DepartEventFactory(handler);

    SessionState updatedState = factory.send(sessionState, routeProgress, location);
    factory.send(updatedState, routeProgress, location);
    factory.reset();
    factory.send(updatedState, routeProgress, location);

    verify(handler, times(2)).send(
      any(SessionState.class), any(MetricsRouteProgress.class), any(MetricsLocation.class)
    );
  }
}