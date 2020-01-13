package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsReporter;
import com.mapbox.services.android.navigation.v5.utils.time.ElapsedTime;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class InitialGpsEventFactoryTest {

  @Test
  public void navigationStarted_elapsedTimeStarts() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    MetricsReporter metricsReporter = mock(MetricsReporter.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(metricsReporter, time, handler);

    factory.navigationStarted("some_session");

    verify(time).start();
  }

  @Test
  public void gpsReceived_elapsedTimeEnds() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    MetricsReporter metricsReporter = mock(MetricsReporter.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(metricsReporter, time, handler);
    NavigationPerformanceMetadata metadata = mock(NavigationPerformanceMetadata.class);

    factory.gpsReceived(metadata);

    verify(time).end();
  }

  @Test
  public void validData_sendsCorrectEvent() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    MetricsReporter metricsReporter = mock(MetricsReporter.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(metricsReporter, time, handler);
    NavigationPerformanceMetadata metadata = mock(NavigationPerformanceMetadata.class);
    String sessionId = "some_session";

    factory.navigationStarted(sessionId);
    waitingForGps();
    factory.gpsReceived(metadata);

    verify(handler).send(anyDouble(), eq(sessionId), eq(metadata));
  }

  @Test
  public void validData_doesNotSendEventTwice() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    MetricsReporter metricsReporter = mock(MetricsReporter.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(metricsReporter, time, handler);
    NavigationPerformanceMetadata metadata = mock(NavigationPerformanceMetadata.class);
    String sessionId = "some_session";

    factory.navigationStarted(sessionId);
    waitingForGps();
    factory.gpsReceived(metadata);
    factory.gpsReceived(metadata);

    verify(handler, times(1)).send(anyDouble(), eq(sessionId), eq(metadata));
  }

  @Test
  public void invalidStart_doesNotSend() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    MetricsReporter metricsReporter = mock(MetricsReporter.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(metricsReporter, time, handler);
    NavigationPerformanceMetadata metadata = mock(NavigationPerformanceMetadata.class);

    factory.gpsReceived(metadata);

    verifyZeroInteractions(handler);
  }

  @Test
  public void reset_allowsNewEventToBeSent() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    MetricsReporter metricsReporter = mock(MetricsReporter.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(metricsReporter, time, handler);
    NavigationPerformanceMetadata metadata = mock(NavigationPerformanceMetadata.class);
    String firstSessionId = "first_session";
    String secondSessionId = "second_session";

    factory.navigationStarted(firstSessionId);
    waitingForGps();
    factory.gpsReceived(metadata);
    factory.gpsReceived(metadata);
    factory.reset();
    factory.navigationStarted(secondSessionId);
    waitingForGps();
    factory.gpsReceived(metadata);

    verify(handler, times(1)).send(anyDouble(), eq(firstSessionId), eq(metadata));
    verify(handler, times(1)).send(anyDouble(), eq(secondSessionId), eq(metadata));
  }

  private void waitingForGps() {
    // Empty operation to simulate waiting for GPS
  }
}