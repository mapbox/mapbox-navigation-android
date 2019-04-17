package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import org.json.JSONObject;

import timber.log.Timber;

class OfflineMetadataProvider {

  private static final String ROUTE_SUMMARY = "route_summary";
  private static final String JSON_CHARSET = "UTF-8";

  @Nullable
  byte[] buildMetaDataFor(String routeSummary) {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(ROUTE_SUMMARY, routeSummary);
      String json = jsonObject.toString();
      return json.getBytes(JSON_CHARSET);
    } catch (Exception exception) {
      Timber.e("Failed to encode metadata: %s", exception.getMessage());
      return null;
    }
  }
}