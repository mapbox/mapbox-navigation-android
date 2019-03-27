package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class NavigationPerformanceMetadata {
  abstract String version();

  abstract String screenSize();

  abstract String country();

  abstract String device();

  abstract String abi();

  abstract String brand();

  abstract String ram();

  abstract String os();

  abstract String gpu();

  abstract String manufacturer();

  static Builder builder() {
    return new AutoValue_NavigationPerformanceMetadata.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder version(String version);

    abstract Builder screenSize(String screenSize);

    abstract Builder country(String country);

    abstract Builder device(String device);

    abstract Builder abi(String abi);

    abstract Builder brand(String brand);

    abstract Builder ram(String ram);

    abstract Builder os(String os);

    abstract Builder gpu(String gpu);

    abstract Builder manufacturer(String manufacturer);

    abstract NavigationPerformanceMetadata build();
  }
}
