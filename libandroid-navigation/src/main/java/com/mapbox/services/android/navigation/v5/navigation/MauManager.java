package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import com.mapbox.android.accounts.v1.MapboxAccounts;

import java.util.HashSet;
import java.util.Set;

class MauManager implements Mau {

  private static final String MAPBOX_NAV_PREFERENCE_MAU_SKU = "com.mapbox.navigationsdk.accounts.mau.sku";
  private static final String MAPBOX_NAV_PREFERENCES_USER_ID = "com.mapbox.navigationsdk.accounts.mau.userid";
  private static final int MAU_TIMER_EXPIRE_THRESHOLD = 1;
  private static final long MAU_TIMER_EXPIRE_AFTER = (DateUtils.HOUR_IN_MILLIS / 1000) * MAU_TIMER_EXPIRE_THRESHOLD;
  private AccountsPreference accountsPreference;
  private Set<Runnable> taskSet = new HashSet<>();
  private Handler handler = new Handler(Looper.getMainLooper());
  private Runnable tokenTimer = new Runnable() {
    @Override
    public void run() {
      refreshToken();
    }
  };

  MauManager(AccountsPreference accountsPreference) {
    this.accountsPreference = accountsPreference;
    addAndStartTask(tokenTimer);
  }

  //TODO figure out when exactly to remove callbacks from this handler.
  private void refreshToken() {
    persistMauSkuToken();
    handler.postDelayed(tokenTimer, MAU_TIMER_EXPIRE_AFTER);
  }

  private void persistMauSkuToken() {
    String token = generateMauSkuToken();
    accountsPreference.getPreferenceManager().set(MAPBOX_NAV_PREFERENCE_MAU_SKU, token);
  }

  private String retrieveMauSkuToken() {
    return accountsPreference.getPreferenceManager().get(MAPBOX_NAV_PREFERENCE_MAU_SKU, "");
  }

  private void persistMauUserId(String userId) {
    accountsPreference.getPreferenceManager().set(MAPBOX_NAV_PREFERENCES_USER_ID, userId);
  }

  private String retrieveUserId() {
    return accountsPreference.getPreferenceManager().get(MAPBOX_NAV_PREFERENCES_USER_ID, "");
  }

  private String generateUserId() {
    return MapboxAccounts.obtainEndUserId();
  }

  private String generateMauSkuToken() {
    String userId = retrieveUserId();
    if (TextUtils.isEmpty(userId)) {
      userId = generateUserId();
      persistMauUserId(userId);
    }
    return MapboxAccounts.obtainNavigationSkuUserToken(userId);
  }

  private void addAndStartTask(Runnable runnable) {
    if (taskSet.isEmpty()) {
      taskSet.add(runnable);
      refreshToken();
    }
  }

  @NonNull
  @Override
  public String obtainSkuToken() {
    addAndStartTask(tokenTimer);
    return retrieveMauSkuToken();
  }

  @Override
  public void onEndNavigation() {
    taskSet.remove(tokenTimer);
    handler.removeCallbacks(tokenTimer);
  }
}
