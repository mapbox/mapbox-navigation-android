package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.BannerComponents;

class ImageVerifier extends NodeVerifier {

  @Override
  boolean isNodeType(BannerComponents bannerComponents) {
    return hasImageUrl(bannerComponents);
  }

  boolean hasImageUrl(BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }
}
