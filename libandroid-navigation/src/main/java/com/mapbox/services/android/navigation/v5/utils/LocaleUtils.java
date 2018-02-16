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
   * Returns the device locale to default to if no locale was specified
   * @param context to check configuration
   * @return locale of device
   */
  public static Locale getDeviceLocale(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return context.getResources().getConfiguration().getLocales().get(0);
    } else {
      return context.getResources().getConfiguration().locale;
    }
  }

  /**
   * Returns the locale passed in if it is not null, otherwise returns the device locale
   * @param context to get device locale
   * @param locale to check if it is null
   * @return a non-null locale, either the one passed in, or the device locale
   */
  public static Locale getNonNullLocale(Context context, Locale locale) {
    if (locale == null) {
      return getDeviceLocale(context);
    }
    return locale;
  }
}
