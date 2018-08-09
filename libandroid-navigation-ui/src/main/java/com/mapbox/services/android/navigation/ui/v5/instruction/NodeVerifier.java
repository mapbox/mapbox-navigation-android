package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

abstract class NodeVerifier {
  abstract boolean isNodeType(BannerComponents bannerComponents);
}
