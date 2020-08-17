package com.mapbox.navigation.ui.instruction;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerComponents;

class ImageVerifier implements NodeVerifier {

  @Override
  public boolean isNodeType(@NonNull BannerComponents bannerComponents) {
    return hasImageUrl(bannerComponents);
  }

  boolean hasImageUrl(@NonNull BannerComponents components) {
    return !TextUtils.isEmpty(components.imageBaseUrl());
  }
}
