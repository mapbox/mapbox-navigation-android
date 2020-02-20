package com.mapbox.navigation.ui.utils;

import com.mapbox.navigation.base.typedef.VoiceUnit;

import java.util.Locale;

import static com.mapbox.navigation.base.typedef.VoiceUnitKt.IMPERIAL;
import static com.mapbox.navigation.base.typedef.VoiceUnitKt.METRIC;

public final class LocaleEx {

  /**
   * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
   * all methods consuming unit type are able to handle the NONE_SPECIFIED type
   *
   * @return unit type for specified locale
   */
  @VoiceUnit
  public static String getUnitTypeForLocale(Locale locale) {
    if (locale.getCountry().equalsIgnoreCase("US")
      || locale.getCountry().equalsIgnoreCase("LR")
      || locale.getCountry().equalsIgnoreCase("MM")) {
      return IMPERIAL;
    }
    return METRIC;
  }
}
