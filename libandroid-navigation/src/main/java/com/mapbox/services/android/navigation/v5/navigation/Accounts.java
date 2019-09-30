package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.NonNull;

interface Accounts {
  @NonNull
  String obtainSkuToken();

  void onEndNavigation();
}
