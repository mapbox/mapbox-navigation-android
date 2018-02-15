package com.mapbox.services.android.navigation.v5.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType.TYPE_IMPERIAL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType.TYPE_METRIC;

public class LocaleUtils {

  /**
   * Returns the unit type for the specified locale
   * @param locale for which to return the default unit type
   * @return unit type for specified locale
   */
  public static @NavigationUnitType.UnitType int getUnitTypeForLocale(@NonNull Locale locale) {
    switch (locale.getCountry()) {
      case "US": // US
      case "LR": // Liberia
      case "MM": // Burma
        return TYPE_IMPERIAL;
      default:
        return TYPE_METRIC;
    }
  }

  /**
   *
   * @param context
   * @return
   */
  public static Locale getDeviceLocale(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return context.getResources().getConfiguration().getLocales().get(0);
    } else {
      return context.getResources().getConfiguration().locale;
    }
  }
}
