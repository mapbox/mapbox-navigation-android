package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

class TextVerifier extends NodeVerifier {
  @Override
  boolean isNodeType(BannerComponents bannerComponents) {
    return bannerComponents.text() != null && !bannerComponents.text().isEmpty();
  }
}
