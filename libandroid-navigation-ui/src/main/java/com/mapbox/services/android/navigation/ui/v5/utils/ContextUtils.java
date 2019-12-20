package com.mapbox.services.android.navigation.ui.v5.utils;

import android.content.Context;
import android.os.Build;

import java.util.Locale;

public class ContextUtils {
  /**
   * Returns the device language to default to if no locale was specified
   *
   * @return language of device
   */
  public static String inferDeviceLanguage(Context context) {
    return inferDeviceLocale(context).getLanguage();
  }

  /**
   * Returns the device locale for which to use as a default if no language is specified
   *
   * @return locale of device
   */
  public static Locale inferDeviceLocale(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return context.getResources().getConfiguration().getLocales().get(0);
    } else {
      return context.getResources().getConfiguration().locale;
    }
  }
}
