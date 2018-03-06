package com.mapbox.services.android.navigation.ui.v5.voice;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SpeechOptions {

  @Nullable
  public abstract String language();

  @Nullable
  public abstract String textType();

  @Nullable
  public abstract String outputType();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder language(String language);

    public abstract Builder textType(String textType);

    public abstract Builder outputType(String outputType);

    public abstract SpeechOptions build();
  }

  public static Builder builder() {
    return new AutoValue_SpeechOptions.Builder();
  }
}
