package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class InitialGpsEventFactoryTest {
  @Mock
  NavigationPerformanceMetadata metadata;
  @Mock
  Context context;
  @Mock
  Resources resources;
  @Mock
  DisplayMetrics displayMetrics;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(context.getResources()).thenReturn(resources);
    when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
    when(displayMetrics.widthPixels).thenReturn(100);
    when(displayMetrics.heightPixels).thenReturn(200);
  }

  @Test
  public void navigationStarted_elapsedTimeStarts() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler, metadata);

    factory.navigationStarted("some_session");

    verify(time).start();
  }

  @Test
  public void gpsReceived_elapsedTimeEnds() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler, metadata);

    factory.gpsReceived(context);

    verify(time).end();
  }

  @Test
  public void validData_sendsCorrectEvent() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    String sessionId = "some_session";
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler, metadata);

    factory.navigationStarted(sessionId);
    waitingForGps();
    factory.gpsReceived(context);

    verify(handler).send(any(Context.class), anyDouble(), eq(sessionId),
      any(NavigationPerformanceMetadata.class));
  }

  @Test
  public void validData_doesNotSendEventTwice() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    String sessionId = "some_session";
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler, metadata);

    factory.navigationStarted(sessionId);
    waitingForGps();
    factory.gpsReceived(context);
    factory.gpsReceived(context);

    verify(handler, times(1)).send(any(Context.class), anyDouble(), eq(sessionId),
      any(NavigationPerformanceMetadata.class));
  }

  @Test
  public void invalidStart_doesNotSend() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler, metadata);

    factory.gpsReceived(context);

    verifyZeroInteractions(handler);
  }

  @Test
  public void reset_allowsNewEventToBeSent() {
    ElapsedTime time = mock(ElapsedTime.class);
    InitialGpsEventHandler handler = mock(InitialGpsEventHandler.class);
    String firstSessionId = "first_session";
    String secondSessionId = "second_session";
    InitialGpsEventFactory factory = new InitialGpsEventFactory(time, handler, metadata);

    factory.navigationStarted(firstSessionId);
    waitingForGps();
    factory.gpsReceived(context);
    factory.gpsReceived(context);
    factory.reset();
    factory.navigationStarted(secondSessionId);
    waitingForGps();
    factory.gpsReceived(context);

    verify(handler, times(1)).send(any(Context.class), anyDouble(), eq(firstSessionId),
      any(NavigationPerformanceMetadata.class));
    verify(handler, times(1)).send(any(Context.class), anyDouble(), eq(secondSessionId),
      any(NavigationPerformanceMetadata.class));
  }

  private void waitingForGps() {
    // Empty operation to simulate waiting for GPS
  }
}