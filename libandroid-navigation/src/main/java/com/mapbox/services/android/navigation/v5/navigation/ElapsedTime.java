package com.mapbox.services.android.navigation.v5.navigation;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.v5.exception.NavigationException;

@AutoValue
abstract class ElapsedTime {

  abstract long getElapsedTime();

  public static Builder builder() {
    return new AutoValue_ElapsedTime.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    private Long start = null;
    private Long end = null;

    Builder start() {
      start = System.nanoTime();
      return this;
    }

    long getStart() {
      return start;
    }

    Builder end() {
      if (start == null) {
        throw new NavigationException("Must call start() before calling end()");
      }
      end = System.nanoTime();
      return this;
    }

    long getEnd() {
      return end;
    }

    abstract Builder elapsedTime(long elapsedTime);

    abstract ElapsedTime autoBuild();

    ElapsedTime build() {
      elapsedTime(end - start);
      return autoBuild();
    }
  }
}
