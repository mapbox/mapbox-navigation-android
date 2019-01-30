package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.core.utils.TextUtils;

class AbbreviationVerifier implements NodeVerifier {
  @Override
  public boolean isNodeType(BannerComponents bannerComponents) {
    return hasAbbreviation(bannerComponents);
  }

  private boolean hasAbbreviation(BannerComponents components) {
    return !TextUtils.isEmpty(components.abbreviation());
  }
}
