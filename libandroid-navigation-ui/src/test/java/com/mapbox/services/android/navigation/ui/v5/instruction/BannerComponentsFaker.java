package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

class BannerComponentsFaker {
  static BannerComponents.Builder bannerComponents() {
    return BannerComponents.builder()
      .type("some type")
      .text("some text");
  }

  static BannerComponents bannerComponentsWithAbbreviation() {
    return bannerComponents()
      .abbreviationPriority(1)
      .abbreviation("abbreviation text")
      .build();
  }
}
