package com.mapbox.navigation.ui.map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class MapBatteryMonitor {

  private static final int DEFAULT_BATTERY_LEVEL = -1;

  boolean isPluggedIn(@NonNull Context context) {
    Intent batteryStatus = registerBatteryUpdates(context);
    if (batteryStatus == null) {
      return false;
    }

    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, DEFAULT_BATTERY_LEVEL);
    boolean pluggedUsb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    boolean pluggedAc = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    boolean isPlugged = pluggedUsb || pluggedAc;
    isPlugged = isPlugged || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    return isPlugged;
  }

  @Nullable
  private static Intent registerBatteryUpdates(@NonNull Context context) {
    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    return context.registerReceiver(null, filter);
  }
}