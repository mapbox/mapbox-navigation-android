package com.mapbox.navigation.ui.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

interface NodeVerifier {
  boolean isNodeType(BannerComponents bannerComponents);
}
