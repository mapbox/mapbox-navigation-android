package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;

import com.mapbox.api.directions.v5.models.BannerComponents;

class BannerShieldInfo {
  private String url;
  private String text;
  private int nodeIndex;
  private int startIndex = -1;

  BannerShieldInfo(Context context, BannerComponents bannerComponents, int nodeIndex) {
    this.url = new UrlDensityMap(context).get(bannerComponents.imageBaseUrl());
    this.nodeIndex = nodeIndex;
    this.text = bannerComponents.text();
  }

  String getUrl() {
    return url;
  }

  public String getText() {
    return text;
  }

  public int getNodeIndex() {
    return nodeIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public int getStartIndex() {
    return startIndex;
  }

  int getEndIndex() {
    return startIndex + text.length();
  }
}
