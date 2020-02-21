package com.mapbox.navigation.ui;

import com.mapbox.navigation.ui.NavigationPresenter;
import com.mapbox.navigation.ui.NavigationViewWayNameListener;
import com.mapbox.navigation.ui.map.OnWayNameChangedListener;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationViewWayNameListenerTest {

  @Test
  public void onWayNameChanged_presenterReceivesNewWayName() {
    NavigationPresenter presenter = mock(NavigationPresenter.class);
    String newWayName = "New way name";
    OnWayNameChangedListener listener = new NavigationViewWayNameListener(presenter);

    listener.onWayNameChanged(newWayName);

    verify(presenter).onWayNameChanged(newWayName);
  }
}