package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

class BatteryMonitor {

  private static final int UNAVAILABLE_BATTERY_LEVEL = -1;
  private static final int DEFAULT_BATTERY_LEVEL = -1;
  private static final int DEFAULT_SCALE = 100;
  private static final float PERCENT_SCALE = 100.0f;
  private final SdkVersionChecker jellyBeanVersionChecker;

  BatteryMonitor() {
    this.jellyBeanVersionChecker = new SdkVersionChecker(Build.VERSION_CODES.JELLY_BEAN);
  }

  BatteryMonitor(SdkVersionChecker jellyBeanVersionChecker) {
    this.jellyBeanVersionChecker = jellyBeanVersionChecker;
  }

  float obtainPercentage(Context context) {
    Intent batteryStatus = registerBatteryUpdates(context);
    if (batteryStatus == null) {
      return UNAVAILABLE_BATTERY_LEVEL;
    }
    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, DEFAULT_BATTERY_LEVEL);
    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, DEFAULT_SCALE);
    return (level / (float) scale) * PERCENT_SCALE;
  }

  boolean isPluggedIn(Context context) {
    Intent batteryStatus = registerBatteryUpdates(context);
    if (batteryStatus == null) {
      return false;
    }

    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, DEFAULT_BATTERY_LEVEL);
    boolean pluggedUsb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    boolean pluggedAc = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    boolean pluggedWireless = false;
    if (jellyBeanVersionChecker.isGreaterThan(Build.VERSION.SDK_INT)) {
      pluggedWireless = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }
    boolean isPlugged = pluggedUsb || pluggedAc || pluggedWireless;
    return isPlugged;
  }

  private Intent registerBatteryUpdates(Context context) {
    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    return context.registerReceiver(null, filter);
  }
}
