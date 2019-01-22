package com.mapbox.services.android.navigation.v5.navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.lang.ref.WeakReference;

class EndNavigationBroadcastReceiver extends BroadcastReceiver {

  private final WeakReference<MapboxNavigation> navigationWeakReference;
  static final String END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION";

  EndNavigationBroadcastReceiver(MapboxNavigation navigation) {
    this.navigationWeakReference = new WeakReference<>(navigation);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    MapboxNavigation navigation = navigationWeakReference.get();
    if (navigation != null) {
      navigation.stopNavigation();
    }
  }
}
