package com.mapbox.services.android.navigation.v5.utils.time;


import java.util.Calendar;
import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.utils.time.TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT;
import static com.mapbox.services.android.navigation.v5.utils.time.TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT;

class NoneSpecifiedTimeFormat implements TimeFormatResolver {
  private final boolean isDeviceTwentyFourHourFormat;

  NoneSpecifiedTimeFormat(boolean isDeviceTwentyFourHourFormat) {
    this.isDeviceTwentyFourHourFormat = isDeviceTwentyFourHourFormat;
  }

  @Override
  public void nextChain(TimeFormatResolver chain) {
  }

  @Override
  public String obtainTimeFormatted(int type, Calendar time) {
    if (isDeviceTwentyFourHourFormat) {
      return String.format(Locale.getDefault(), TWENTY_FOUR_HOURS_FORMAT, time, time);
    } else {
      return String.format(Locale.getDefault(), TWELVE_HOURS_FORMAT, time, time, time);
    }
  }
}
