package com.mapbox.services.android.navigation.v5.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.DirectionsCriteria;

import java.util.Locale;

public class LocaleUtils {

  /**
   * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
   * all methods consuming unit type are able to handle the NONE_SPECIFIED type
   * @param locale for which to return the default unit type
   * @return unit type for specified locale
   */
  @DirectionsCriteria.VoiceUnitCriteria
  public static String getUnitTypeForLocale(@NonNull Locale locale) {
    switch (locale.getCountry()) {
      case "US": // US
      case "LR": // Liberia
      case "MM": // Burma
        return DirectionsCriteria.IMPERIAL;
      default:
        return DirectionsCriteria.METRIC;
    }
  }

  /**
   * Returns the device locale to default to if no locale was specified
   * @param context to check configuration
   * @return locale of device
   */
  public static String getDeviceLanguage(Context context) {
    return getDeviceLocale(context).getLanguage();
  }


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
   * @param language to check if it is null
   * @return a non-null locale, either the one passed in, or the device locale
   */
  public static String getNonNullLocale(Context context, String language) {
    if (language == null) {
      return getDeviceLanguage(context);
    }
    return language;
  }

  public static String getUnitTypeForDeviceLocale(Context context) {
    return getUnitTypeForLocale(getDeviceLocale(context));
  }
}
