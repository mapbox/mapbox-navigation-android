package com.mapbox.services.android.navigation.v5.utils.time;


import java.util.Calendar;
import java.util.Locale;

class TwentyFourHoursTimeFormat implements TimeFormatResolver {
  static final String TWENTY_FOUR_HOURS_FORMAT = "%tk:%tM";
  private static final int TWENTY_FOUR_HOURS_TYPE = 1;
  private TimeFormatResolver chain;

  @Override
  public void nextChain(TimeFormatResolver chain) {
    this.chain = chain;
  }

  @Override
  public String obtainTimeFormatted(int type, Calendar time) {
    if (type == TWENTY_FOUR_HOURS_TYPE) {
      return String.format(Locale.getDefault(), TWENTY_FOUR_HOURS_FORMAT, time, time);
    } else {
      return chain.obtainTimeFormatted(type, time);
    }
  }
}
