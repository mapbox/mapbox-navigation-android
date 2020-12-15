package com.mapbox.navigation.examples.util;

import android.content.Context;
import androidx.annotation.NonNull;

public class Utils {
  /**
   * <p>
   * Returns the Mapbox access token set in the app resources.
   * </p>
   *
   * @param context The {@link Context} of the {@link android.app.Activity} or {@link android.app.Fragment}.
   * @return The Mapbox access token or null if not found.
   */
  public static String getMapboxAccessToken(@NonNull Context context) {
    int tokenResId = context.getResources()
        .getIdentifier("mapbox_access_token", "string", context.getPackageName());
    return tokenResId != 0 ? context.getString(tokenResId) : null;
  }
}
