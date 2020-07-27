package com.mapbox.navigation.ui.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

class BannerComponentsFaker {
  static BannerComponents bannerComponents() {
    return bannerComponentsBuilder().build();
  }

  static BannerComponents.Builder bannerComponentsBuilder() {
    return BannerComponents.builder()
      .type("some type")
      .text("some text");
  }

  static BannerComponents bannerComponentsWithAbbreviation() {
    return bannerComponentsBuilder()
      .abbreviationPriority(1)
      .abbreviation("abbreviation text")
      .build();
  }

  static BannerComponents bannerComponentsWithNullGuidanceUrl() {
    return BannerComponents.builder()
            .type("guidance-view")
            .text("some text")
            .imageUrl(null)
            .build();
  }

  static BannerComponents bannerComponentsWithGuidanceUrlNoAccessToken(String path) {
    return BannerComponents.builder()
            .type("guidance-view")
            .text("some text")
            .imageUrl(path)
            .build();
  }
}
