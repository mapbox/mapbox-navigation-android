package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.Keep;

@Keep
class Attribute {
  private final String name;
  private final String value;

  Attribute(String name, String value) {
    this.name = name;
    this.value = value;
  }
}
