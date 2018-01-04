package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;

class BannerShieldInfo {
  private String url;
  private String text;
  private int startIndex;

  BannerShieldInfo(Context context, String url, int startIndex, String text) {
    this.url = new UrlDensityMap(context).get(url);
    this.startIndex = startIndex;
    this.text = text;
  }

  String getUrl() {
    return url;
  }

  public String getText() {
    return text;
  }

  int getStartIndex() {
    return startIndex;
  }

  int getEndIndex() {
    return startIndex + 1;
  }
}
