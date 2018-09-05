package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.api.directions.v5.models.BannerInstructions;

class BannerInstructionsWrapper {
  private final BannerInstructions bannerInstructions;
  private final boolean isBannerTextOverridden;

  BannerInstructionsWrapper(BannerInstructions bannerInstructions, boolean isBannerTextOverridden) {
    this.bannerInstructions = bannerInstructions;
    this.isBannerTextOverridden = isBannerTextOverridden;
  }

  public BannerInstructions getBannerInstructions() {
    return bannerInstructions;
  }

  public boolean isBannerTextOverridden() {
    return isBannerTextOverridden;
  }
}
