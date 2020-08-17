package com.mapbox.navigation.ui.instruction;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerComponents;

/**
 * Class used to construct a list of BannerComponents to be populated into a TextView
 */
class BannerComponentNode {
  BannerComponents bannerComponents;
  int startIndex;

  BannerComponentNode(BannerComponents bannerComponents, int startIndex) {
    this.bannerComponents = bannerComponents;
    this.startIndex = startIndex;
  }

  @NonNull
  @Override
  public String toString() {
    return bannerComponents.text();
  }

  void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }
}
