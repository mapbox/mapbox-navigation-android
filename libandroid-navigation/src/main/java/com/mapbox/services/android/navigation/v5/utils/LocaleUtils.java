package com.mapbox.services.android.navigation.v5.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType.NONE_SPECIFIED;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType.TYPE_IMPERIAL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType.TYPE_METRIC;

public class LocaleUtils {

  /**
   * Returns the locale specified in SharedPreferences, or the device Locale if not specified
   * @param context where SharedPreferences are stored
   * @return locale specified in SharedPrefs, or the device locale
   */
  public static Locale getLocale(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    String localeLanguage = preferences.getString(
      NavigationConstants.NAVIGATION_VIEW_LOCALE_LANGUAGE, "");
    String localeCountry = preferences.getString(
      NavigationConstants.NAVIGATION_VIEW_LOCALE_COUNTRY, "");

    if (!localeCountry.isEmpty() && !localeLanguage.isEmpty()) {
      return new Locale(localeLanguage, localeCountry);
    } else if (!localeLanguage.isEmpty()) { // Country is not required for a locale
      return new Locale(localeLanguage);
    } else { // If locale isn't specified, use device locale
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return context.getResources().getConfiguration().getLocales().get(0);
      } else {
        return context.getResources().getConfiguration().locale;
      }
    }
  }

  /**
   * Sets locale into SharedPreferences for later use
   * @param context where SharedPreferences exist
   * @param locale to set language
   */
  public static void setLocale(Context context, Locale locale) {
    setLocale(context, locale, NavigationUnitType.NONE_SPECIFIED);
  }

  /**
   * Sets locale and unitType into SharedPreferences for later use
   * @param context where SharedPreferences exist
   * @param locale to set language
   * @param unitType to set unitType to use
   */
  public static void setLocale(Context context, Locale locale, @NavigationUnitType.UnitType int unitType) {
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

    setLocale(editor, locale, unitType);

    editor.apply();
  }

  /**
   * Sets locale and unitType into SharedPreferences for later use
   * @param editor to add locale and unitType to
   * @param locale to set language
   * @param unitType to set unitType to use
   */
  public static void setLocale(
    SharedPreferences.Editor editor, Locale locale, @NavigationUnitType.UnitType int unitType) {
    editor.putInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE, unitType);
    if (locale != null) {
      editor.putString(NavigationConstants.NAVIGATION_VIEW_LOCALE_LANGUAGE, locale.getLanguage());
      editor.putString(NavigationConstants.NAVIGATION_VIEW_LOCALE_COUNTRY, locale.getCountry());
    }
  }

  /**
   * Returns the unit type to use, or the unit type for specified locale if no unit type is specified
   * @param context where SharedPreferences are stored
   * @param locale to use for default
   * @return unit type
   */
  public static @NavigationUnitType.UnitType int getUnitType(Context context, @NonNull Locale locale) {
    int unitType = PreferenceManager.getDefaultSharedPreferences(context)
      .getInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE, NONE_SPECIFIED);

    if (unitType != NONE_SPECIFIED) {
      return unitType;
    }

    // If unit type isn't specified, use default for locale
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
   * Returns the unit type to use, or the unit type for specified locale if no unit type is specified
   * @param context where SharedPreferences are stored
   * @return unit type
   */
  public static @NavigationUnitType.UnitType int getUnitType(Context context) {
    return getUnitType(context, getLocale(context));
  }
}
