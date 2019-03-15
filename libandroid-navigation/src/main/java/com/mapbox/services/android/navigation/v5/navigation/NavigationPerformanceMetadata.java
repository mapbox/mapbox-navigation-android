package com.mapbox.services.android.navigation.v5.navigation;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

class NavigationPerformanceMetadata {
  private static final String OPERATING_SYSTEM = "Android - " + Build.VERSION.RELEASE;
  private static final String DEVICE = Build.DEVICE;
  private static final String MANUFACTURER = Build.MANUFACTURER;
  private static final String BRAND = Build.BRAND;
  private static final String ABI = Build.CPU_ABI;
  private static final String VERSION = String.valueOf(Build.VERSION.SDK_INT);

  private final String version;
  private final String screenSize;
  private final String country;
  private final String device;
  private final String abi;
  private final String brand;
  private final String ram;
  private final String os;
  private final String gpu;
  private final String manufacturer;

  public NavigationPerformanceMetadata(Context context) {
    this.version = VERSION;
    this.screenSize = getScreenSize(context);
    this.country = getCountry(context);
    this.device = DEVICE;
    this.abi = ABI;
    this.brand = BRAND;
    this.ram = getTotalMemory(context);
    this.os = OPERATING_SYSTEM;
    this.gpu = "";
    this.manufacturer = MANUFACTURER;
  }

  private String getTotalMemory(Context context) {
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
    return String.valueOf(memoryInfo.totalMem /(1024 * 1024));
  }

  private String getCountry(Context context) {
    return context.getResources().getConfiguration().locale.getCountry();
  }

  private String getScreenSize(Context context) {
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    return String.format("%dx%d", displayMetrics.widthPixels, displayMetrics.heightPixels);
  }
}
