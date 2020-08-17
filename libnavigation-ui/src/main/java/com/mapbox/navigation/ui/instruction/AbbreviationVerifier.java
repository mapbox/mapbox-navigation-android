package com.mapbox.navigation.ui.instruction;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.core.utils.TextUtils;

class AbbreviationVerifier implements NodeVerifier {
  @Override
  public boolean isNodeType(@NonNull BannerComponents bannerComponents) {
    return hasAbbreviation(bannerComponents);
  }

  private boolean hasAbbreviation(@NonNull BannerComponents components) {
    return !TextUtils.isEmpty(components.abbreviation());
  }
}
