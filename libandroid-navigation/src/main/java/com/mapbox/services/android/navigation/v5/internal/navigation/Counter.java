package com.mapbox.services.android.navigation.v5.internal.navigation;

abstract class Counter<N extends Number> {
  protected final String name;
  protected final N value;

  Counter(String name, N value) {
    this.name = name;
    this.value = value;
  }
}
