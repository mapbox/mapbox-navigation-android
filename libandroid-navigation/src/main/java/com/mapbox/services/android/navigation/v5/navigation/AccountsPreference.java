package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import java.lang.ref.WeakReference;

class AccountsPreference {
  private static final String MAPBOX_NAV_PREFERENCES = "mapbox.navigation.preferences";
  private final PreferenceManager preferenceManager;

  AccountsPreference(WeakReference<Context> context) {
    this.preferenceManager = new PreferenceManager(context.get(), MAPBOX_NAV_PREFERENCES);
  }

  PreferenceManager getPreferenceManager() {
    return preferenceManager;
  }
}
