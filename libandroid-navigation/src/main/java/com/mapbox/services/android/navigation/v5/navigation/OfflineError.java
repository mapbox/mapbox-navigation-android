package com.mapbox.services.android.navigation.v5.navigation;

public class OfflineError {

  private final String message;

  OfflineError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
