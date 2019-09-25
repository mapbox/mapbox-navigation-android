package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import timber.log.Timber;

import java.lang.ref.WeakReference;

class AccountsManagerImpl {

  private static AccountsManager accountsManager;
  private static AccountsManagerImpl INSTANCE;
  private static AccountsPreference accountsPreference;
  private static final String ENABLE_MAU = "EnableMAU";
  private static final String META_DATA = "com.mapbox.services.android.navigation.v5";
  private static final String ENABLE_MAU_META_DATA = META_DATA + ENABLE_MAU;

  static synchronized AccountsManagerImpl getInstance(Context context) {
    if (INSTANCE == null) {
      INSTANCE = new AccountsManagerImpl(context);
    }
    return INSTANCE;
  }

  private AccountsManagerImpl(@NonNull Context ctx) {
    WeakReference<Context> context = new WeakReference<>(ctx);
    accountsPreference = new AccountsPreference(context);
    setBillingModel(context.get());
  }

  private ApplicationInfo getApplicationInfo(@NonNull Context context) {
    ApplicationInfo applicationInfo = null;
    try {
      applicationInfo = context
        .getPackageManager()
        .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException exception) {
      Timber.e(exception);
    }
    return applicationInfo;
  }

  private boolean isMauBillingEnabled(@NonNull Context context) {
    ApplicationInfo applicationInfo = getApplicationInfo(context);
    if (applicationInfo != null && applicationInfo.metaData != null) {
      return applicationInfo.metaData.getBoolean(ENABLE_MAU_META_DATA, false);
    }
    return false;
  }

  private void setBillingModel(@NonNull Context context) {
    accountsManager = new DisableSkuManagerImpl();
    // TODO uncomment when ready to use as a part of 1.0
    /*if (isMauBillingEnabled(context)) {
      accountsManager = new MauManagerImpl(accountsPreference);
    } else {
      accountsManager = new TripsManagerImpl(accountsPreference);
    }*/
  }

  static String obtainSku() {
    return accountsManager.obtainSkuToken();
  }
}
