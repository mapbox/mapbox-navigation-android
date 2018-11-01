package testapp.rules;

import android.support.annotation.NonNull;

public class BatteryStatsDumpsysRule extends AbstractDumpsysRule {

  private static final String BATTERY_STATS = "batterystats";
  private static final String OPTION_CHARGED = "--charged";

  @Override
  protected String dumpsysService() {
    return BATTERY_STATS;
  }

  @NonNull
  @Override
  protected String extraOptions() {
    return OPTION_CHARGED;
  }
}
