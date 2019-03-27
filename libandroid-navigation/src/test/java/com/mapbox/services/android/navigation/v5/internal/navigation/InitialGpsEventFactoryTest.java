package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.services.android.navigation.v5.internal.navigation.InitialGpsEventFactory;
import com.mapbox.services.android.navigation.v5.internal.navigation.InitialGpsEventHandler;

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
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler);

    factory.navigationStarted("some_session");

    verify(time).start();
  }

  @Test
  public void gpsReceived_elapsedTimeEnds() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler);

    factory.gpsReceived();

    verify(time).end();
  }

  @Test
  public void validData_sendsCorrectEvent() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    String sessionId = "some_session";
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler);

    factory.navigationStarted(sessionId);
    waitingForGps();
    factory.gpsReceived();

    verify(handler).send(anyDouble(), eq(sessionId));
  }

  @Test
  public void validData_doesNotSendEventTwice() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    String sessionId = "some_session";
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler);

    factory.navigationStarted(sessionId);
    waitingForGps();
    factory.gpsReceived();
    factory.gpsReceived();

    verify(handler, times(1)).send(anyDouble(), eq(sessionId));
  }

  @Test
  public void invalidStart_doesNotSend() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler);

    factory.gpsReceived();

    verifyZeroInteractions(handler);
  }

  @Test
  public void reset_allowsNewEventToBeSent() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    String firstSessionId = "first_session";
    String secondSessionId = "second_session";
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler);

    factory.navigationStarted(firstSessionId);
    waitingForGps();
    factory.gpsReceived();
    factory.gpsReceived();
    factory.reset();
    factory.navigationStarted(secondSessionId);
    waitingForGps();
    factory.gpsReceived();

    verify(handler, times(1)).send(anyDouble(), eq(firstSessionId));
    verify(handler, times(1)).send(anyDouble(), eq(secondSessionId));
  }

  private void waitingForGps() {
    // Empty operation to simulate waiting for GPS
  }
}