package com.mapbox.services.android.navigation.v5.utils.time;


import java.util.Calendar;
import java.util.Locale;

class TwelveHoursTimeFormat implements TimeFormatResolver {
  static final String TWELVE_HOURS_FORMAT = "%tl:%tM %tp";
  private static final int TWELVE_HOURS_TYPE = 0;
  private TimeFormatResolver chain;

  @Override
  public void nextChain(TimeFormatResolver chain) {
    this.chain = chain;
  }

  @Override
  public String obtainTimeFormatted(int type, Calendar time) {
    if (type == TWELVE_HOURS_TYPE) {
      return String.format(Locale.getDefault(), TWELVE_HOURS_FORMAT, time, time, time);
    } else {
      return chain.obtainTimeFormatted(type, time);
    }
  }
}
