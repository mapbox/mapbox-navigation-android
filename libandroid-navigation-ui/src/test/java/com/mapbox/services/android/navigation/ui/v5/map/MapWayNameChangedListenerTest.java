package com.mapbox.services.android.navigation.ui.v5.map;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MapWayNameChangedListenerTest {

  @Test
  public void onWayNameChanged_listenersAreUpdated() {
    List<OnWayNameChangedListener> listeners = new ArrayList<>();
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);
    listeners.add(listener);
    MapWayNameChangedListener wayNameChangedListener = new MapWayNameChangedListener(listeners);
    String someWayName = "some way name";

    wayNameChangedListener.onWayNameChanged(someWayName);

    verify(listener).onWayNameChanged(eq(someWayName));
  }
}