package com.mapbox.services.android.navigation.v5.navigation;

import com.google.gson.annotations.SerializedName;

class OfflineError {

  private final String status;
  @SerializedName("status_code")
  private final int statusCode;
  private final String error;
  @SerializedName("error_code")
  private final int errorCode;

  OfflineError(String status, int statusCode, String error, int errorCode) {
    this.status = status;
    this.statusCode = statusCode;
    this.error = error;
    this.errorCode = errorCode;
  }

  public String getStatus() {
    return status;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getError() {
    return error;
  }

  public int getErrorCode() {
    return errorCode;
  }
}
