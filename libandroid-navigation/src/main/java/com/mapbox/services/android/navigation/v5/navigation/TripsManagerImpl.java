package com.mapbox.services.android.navigation.v5.navigation;

import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import com.mapbox.android.accounts.v1.MapboxAccounts;

class TripsManagerImpl implements TripsManager {

  enum RotateTripSku {
    INVALID,
    ROTATE_ON_TIMER_EXPIRE,
    ROTATE_ON_REQUEST_COUNT_EXPIRE
  }

  private static final String MAPBOX_NAV_PREFERENCE_TRIPS_SKU = "com.mapbox.navigationsdk.accounts.trips.sku";
  private static final String MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT = "com.mapbox.navigationsdk.accounts.trips.count";
  private static final String MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP = "com.mapbox.navigationsdk.accounts.trips.time";
  private static final int ROUTE_REQUEST_COUNT_THRESHOLD = 5;
  private static final int TRIPS_TIMER_EXPIRE_THRESHOLD = 2;
  private static final long TRIPS_TIMER_EXPIRE_AFTER = (DateUtils.HOUR_IN_MILLIS / 1000) * TRIPS_TIMER_EXPIRE_THRESHOLD;
  private RotateTripSku rotateSku = RotateTripSku.INVALID;
  private AccountsPreference accountsPreference;

  TripsManagerImpl(AccountsPreference accountsPreference) {
    this.accountsPreference = accountsPreference;
  }

  private void refreshSkuToken() {
    if (!shouldRefreshSku()) {
      return;
    }
    int requestCount;
    switch (rotateSku) {
      case ROTATE_ON_TIMER_EXPIRE:
        requestCount = getRouteRequestCountThreshold();
        requestCount++;
        setTimerExpiry();
        break;
      case ROTATE_ON_REQUEST_COUNT_EXPIRE:
        requestCount = 0;
        setTimerExpiry();
        break;
      default:
        requestCount = getRouteRequestCountThreshold();
        requestCount++;
        break;
    }
    setRouteRequestCountThreshold(requestCount);
    persistTripsSkuToken();
  }

  private boolean shouldRefreshSku() {
    boolean routeReqCountExpired = validateRouteRequestCountExpiry();
    boolean timerExpired = validateTimerExpiry();
    if (routeReqCountExpired) {
      rotateSku = RotateTripSku.ROTATE_ON_REQUEST_COUNT_EXPIRE;
    } else if (timerExpired) {
      rotateSku = RotateTripSku.ROTATE_ON_TIMER_EXPIRE;
    } else {
      rotateSku = RotateTripSku.INVALID;
    }
    return routeReqCountExpired || timerExpired;
  }

  private boolean validateTimerExpiry() {
    long skuTokenTimeStamp = getTimerExpiry();
    return isTwoHoursExpired(skuTokenTimeStamp);
  }

  private boolean validateRouteRequestCountExpiry() {
    int routeRequestCount = getRouteRequestCountThreshold();
    return routeRequestCount > ROUTE_REQUEST_COUNT_THRESHOLD;
  }

  private void setRouteRequestCountThreshold(int count) {
    accountsPreference.getPreferenceManager().set(MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT, count);
  }

  private int getRouteRequestCountThreshold() {
    return accountsPreference.getPreferenceManager().get(MAPBOX_NAV_PREFERENCE_ROUTE_REQ_COUNT, 0);
  }

  private void persistTripsSkuToken() {
    String token = generateTripsSkuToken();
    accountsPreference.getPreferenceManager().set(MAPBOX_NAV_PREFERENCE_TRIPS_SKU, token);
  }

  private String retrieveTripsSkuToken() {
    return accountsPreference.getPreferenceManager().get(MAPBOX_NAV_PREFERENCE_TRIPS_SKU, "");
  }

  private String generateTripsSkuToken() {
    return MapboxAccounts.obtainNavigationSkuSessionToken();
  }

  private void setTimerExpiry() {
    accountsPreference.getPreferenceManager().set(MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP, getNow());
  }

  private long getTimerExpiry() {
    return accountsPreference.getPreferenceManager().get(MAPBOX_NAV_PREFERENCE_TRIPS_TIMESTAMP, 0L);
  }

  private boolean isTwoHoursExpired(long then) {
    return isExpired(getNow(), then);
  }

  private long getNow() {
    return System.currentTimeMillis();
  }

  private boolean isExpired(long now, long then) {
    return (now - then) > TRIPS_TIMER_EXPIRE_AFTER;
  }

  @NonNull
  @Override
  public String obtainSkuToken() {
    refreshSkuToken();
    return retrieveTripsSkuToken();
  }
}
