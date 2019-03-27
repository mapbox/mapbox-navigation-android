package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

class MetadataBuilder {

  private static final String OPERATING_SYSTEM = "Android - " + Build.VERSION.RELEASE;
  private static final String DEVICE = Build.DEVICE;
  private static final String MANUFACTURER = Build.MANUFACTURER;
  private static final String BRAND = Build.BRAND;
  private static final String ABI = Build.CPU_ABI;
  private static final String VERSION = String.valueOf(Build.VERSION.SDK_INT);

  NavigationPerformanceMetadata constructMetadata(Context context) {
    return NavigationPerformanceMetadata.builder()
      .version(VERSION)
      .screenSize(getScreenSize(context))
      .country(getCountry(context))
      .device(DEVICE)
      .abi(ABI)
      .brand(BRAND)
      .ram(getTotalMemory(context))
      .os(OPERATING_SYSTEM)
      .gpu("")
      .manufacturer(MANUFACTURER)
      .build();
  }

  private String getTotalMemory(Context context) {
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
    return String.valueOf(memoryInfo.totalMem / (1024 * 1024));
  }

  private String getCountry(Context context) {
    return context.getResources().getConfiguration().locale.getCountry();
  }

  private String getScreenSize(Context context) {
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    return String.format("%dx%d", displayMetrics.widthPixels, displayMetrics.heightPixels);
  }
}
