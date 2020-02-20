package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

class MapBatteryMonitor {

  private static final int DEFAULT_BATTERY_LEVEL = -1;

  boolean isPluggedIn(Context context) {
    Intent batteryStatus = registerBatteryUpdates(context);
    if (batteryStatus == null) {
      return false;
    }

    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, DEFAULT_BATTERY_LEVEL);
    boolean pluggedUsb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    boolean pluggedAc = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    boolean isPlugged = pluggedUsb || pluggedAc;
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
      isPlugged = isPlugged || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }
    return isPlugged;
  }

  private static Intent registerBatteryUpdates(Context context) {
    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    return context.registerReceiver(null, filter);
  }
}