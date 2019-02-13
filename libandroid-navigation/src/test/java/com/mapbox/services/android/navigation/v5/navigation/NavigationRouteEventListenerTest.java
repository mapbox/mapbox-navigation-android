package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import okhttp3.Call;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationRouteEventListenerTest {

  @Test
  public void callStart_timeStartIsCalled() {
    ElapsedTime time = mock(ElapsedTime.class);
    NavigationRouteEventListener listener = new NavigationRouteEventListener(time);

    listener.callStart(mock(Call.class));

    verify(time).start();
  }

  @Test
  public void callEnd_timeEndIsCalled() {
    ElapsedTime time = mock(ElapsedTime.class);
    NavigationRouteEventListener listener = new NavigationRouteEventListener(time);

    listener.callEnd(mock(Call.class));

    verify(time).end();
  }
}