package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.NonNull;

class DisableSkuManager implements DisableSku {

  @NonNull
  @Override
  public String obtainSkuToken() {
    return "";
  }

  @Override
  public void onEndNavigation() {

  }
}
