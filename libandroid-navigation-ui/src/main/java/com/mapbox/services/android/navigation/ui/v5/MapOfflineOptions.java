package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;

public class MapOfflineOptions {

  private final String databasePath;
  private final String styleUrl;

  /**
   * Add an offline path and style URL for loading an offline map database.
   *
   * @param databaseFilePath to the offline database on the device
   * @param styleUrl         for the offline database data
   */
  public MapOfflineOptions(@NonNull String databaseFilePath, @NonNull String styleUrl) {
    this.databasePath = databaseFilePath;
    this.styleUrl = styleUrl;
  }

  /**
   * The offline path to the offline map database.
   *
   * @return the database path
   */
  @NonNull
  public String getDatabasePath() {
    return databasePath;
  }

  /**
   * The map style URL for the offline map database.
   *
   * @return the style URL
   */
  @NonNull
  public String getStyleUrl() {
    return styleUrl;
  }
}
