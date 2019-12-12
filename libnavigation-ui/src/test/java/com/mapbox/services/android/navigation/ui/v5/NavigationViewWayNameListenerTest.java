package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.navigation.ui.v5.NavigationPresenter;
import com.mapbox.navigation.ui.v5.NavigationViewWayNameListener;
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