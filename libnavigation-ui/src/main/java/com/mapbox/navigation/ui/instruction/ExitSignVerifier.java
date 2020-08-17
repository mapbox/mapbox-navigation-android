package com.mapbox.navigation.ui.instruction;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerComponents;

class ExitSignVerifier implements NodeVerifier {

  @Override
  public boolean isNodeType(@NonNull BannerComponents bannerComponents) {
    return bannerComponents.type().equals("exit") || bannerComponents.type().equals("exit-number");
  }
}
