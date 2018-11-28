package com.mapbox.services.android.navigation.v5.navigation;

public class OfflineData {
  private String result;
  private Status status;

  public enum Status { ROUTER_BEING_INITIALIZED(false), NO_ROUTE_FOUND(false),
    CONFIGURE_ROUTER_RESULT_SUCCESS(true), CONFIGURE_ROUTER_RESULT_FAILURE(false);

    private final boolean success;
    Status(boolean success) {
      this.success = success;
    }

    public boolean isSuccess() {
      return success;
    }
  }

  public OfflineData(Status status) {
    this(status, status.toString());
  }

  public OfflineData(Status status, String result) {
    this.status = status;
    this.result = result;
  }

  public String getResult() {
    return result;
  }

  public Status getStatus() {
    return status;
  }

  public String toString() {
    return "" + status.toString() + " " + status.isSuccess() + " " + result;
  }
}
