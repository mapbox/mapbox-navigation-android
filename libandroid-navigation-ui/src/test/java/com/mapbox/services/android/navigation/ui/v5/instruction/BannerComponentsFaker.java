package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

class BannerComponentsFaker {
  static BannerComponents.Builder bannerComponents() {
    return BannerComponents.builder()
      .type("some type")
      .text("some text");
  }
}
