package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import org.json.JSONObject;

class OfflineMetadataProvider {

  private static final String ROUTE_SUMMARY = "route_summary";
  private static final String JSON_CHARSET = "UTF-8";

  @Nullable
  byte[] buildMetadataFor(String routeSummary) {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(ROUTE_SUMMARY, routeSummary);
      String json = jsonObject.toString();
      return json.getBytes(JSON_CHARSET);
    } catch (Exception exception) {
      return null;
    }
  }
}