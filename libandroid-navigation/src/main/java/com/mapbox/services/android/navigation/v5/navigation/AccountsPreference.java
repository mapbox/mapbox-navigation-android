package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import java.lang.ref.WeakReference;

class AccountsPreference {
  private static final String ACCOUNTS = "accounts";
  private final PreferenceManager preferenceManager;

  AccountsPreference(WeakReference<Context> context) {
    this.preferenceManager = new PreferenceManager(context.get(), ACCOUNTS);
  }

  PreferenceManager getPreferenceManager() {
    return preferenceManager;
  }
}
