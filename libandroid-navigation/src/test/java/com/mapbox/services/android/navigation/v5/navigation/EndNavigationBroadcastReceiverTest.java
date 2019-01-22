package com.mapbox.services.android.navigation.v5.navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EndNavigationBroadcastReceiverTest {

  @Test
  public void onReceive_navigationIsStopped() {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    BroadcastReceiver receiver = new EndNavigationBroadcastReceiver(navigation);

    receiver.onReceive(mock(Context.class), mock(Intent.class));

    verify(navigation).stopNavigation();
  }
}