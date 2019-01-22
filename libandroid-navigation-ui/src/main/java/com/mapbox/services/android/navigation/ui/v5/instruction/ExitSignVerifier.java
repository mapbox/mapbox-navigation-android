package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

class ExitSignVerifier extends NodeVerifier {

  @Override
  boolean isNodeType(BannerComponents bannerComponents) {
    return bannerComponents.type().equals("exit") || bannerComponents.type().equals("exit-number");
  }
}
