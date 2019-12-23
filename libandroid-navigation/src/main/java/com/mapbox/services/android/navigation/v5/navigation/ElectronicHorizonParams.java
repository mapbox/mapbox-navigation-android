package com.mapbox.services.android.navigation.v5.navigation;

public class ElectronicHorizonParams {

  private long delay;
  private long interval;
  private int locationsCacheSize;

  private ElectronicHorizonParams(long delay, long interval, int locationsCacheSize) {
    this.delay = delay;
    this.interval = interval;
    this.locationsCacheSize = locationsCacheSize;
  }

  long getDelay() {
    return delay;
  }

  long getInterval() {
    return interval;
  }

  int getLocationsCacheSize() {
    return locationsCacheSize;
  }

  public static class Builder {
    private static final long ELECTRONIC_HORIZON_DELAY_DEFAULT = 20_000;
    private static final long ELECTRONIC_HORIZON_INTERVAL_DEFAULT = 20_000;
    private static final int LOCATIONS_CACHE_SIZE_DEFAULT = 5;
    private static final int LOCATIONS_CACHE_MIN_SIZE = 2;
    private static final int LOCATIONS_CACHE_MAX_SIZE = 10;

    private long delay = ELECTRONIC_HORIZON_DELAY_DEFAULT;
    private long interval = ELECTRONIC_HORIZON_INTERVAL_DEFAULT;
    private int locationsCacheSize = LOCATIONS_CACHE_SIZE_DEFAULT;

    public Builder delay(long delay) {
      if (delay > 0) {
        this.delay = delay;
      }
      return this;
    }

    public Builder interval(long interval) {
      if (interval > 0) {
        this.interval = interval;
      }
      return this;
    }

    public Builder locationsCacheSize(int locationsCacheSize) {
      if (locationsCacheSize >= LOCATIONS_CACHE_MIN_SIZE && locationsCacheSize <= LOCATIONS_CACHE_MAX_SIZE) {
        this.locationsCacheSize = locationsCacheSize;
      }
      return this;
    }

    public ElectronicHorizonParams build() {
      return new ElectronicHorizonParams(delay, interval, locationsCacheSize);
    }
  }
}
