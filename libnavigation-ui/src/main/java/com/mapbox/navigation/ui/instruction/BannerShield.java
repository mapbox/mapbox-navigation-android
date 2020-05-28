package com.mapbox.navigation.ui.instruction;

import com.mapbox.api.directions.v5.models.BannerComponents;

class BannerShield {
  private String url;
  private String text;
  private int nodeIndex;
  private int startIndex = -1;

  BannerShield(BannerComponents bannerComponents, int nodeIndex) {
    this.url = bannerComponents.imageBaseUrl();
    this.nodeIndex = nodeIndex;
    this.text = bannerComponents.text();
  }

  String getUrl() {
    return url;
  }

  String getText() {
    return text;
  }

  int getNodeIndex() {
    return nodeIndex;
  }

  void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  int getStartIndex() {
    return startIndex;
  }

  int getEndIndex() {
    return startIndex + text.length();
  }
}
