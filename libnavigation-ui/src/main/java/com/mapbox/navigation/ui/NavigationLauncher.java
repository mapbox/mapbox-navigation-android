package com.mapbox.navigation.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.ui.navigation.NavigationConstants;

/**
 * Use this class to launch the navigation UI
 * <p>
 * You can launch the UI a route you have already retrieved from
 * {@link com.mapbox.services.android.navigation.v5.navigation.NavigationRoute}.
 * </p><p>
 * For testing, you can launch with simulation, in which our
 * {@link com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine} will begin
 * following the given {@link DirectionsRoute} once the UI is initialized.
 * </p>
 */
public class NavigationLauncher {

  /**
   * Starts the UI with a {@link DirectionsRoute} already retrieved from
   * {@link com.mapbox.services.android.navigation.v5.navigation.NavigationRoute}
   *
   * @param activity must be launched from another {@link Activity}
   * @param options  with fields to customize the navigation view
   */
  public static void startNavigation(Activity activity, NavigationLauncherOptions options) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    SharedPreferences.Editor editor = preferences.edit();

    storeDirectionsRouteValue(options, editor);
    storeConfiguration(options, editor);

    storeThemePreferences(options, editor);
    storeOfflinePath(options, editor);
    storeOfflineVersion(options, editor);
    if (options.offlineMapOptions() != null) {
      storeOfflineMapDatabasePath(options, editor);
      storeOfflineMapStyleUrl(options, editor);
    }

    editor.apply();

    Intent navigationActivity = new Intent(activity, MapboxNavigationActivity.class);
    storeInitialMapPosition(options, navigationActivity);
    activity.startActivity(navigationActivity);
  }

  /**
   * Used to extract the route used to launch the drop-in UI.
   * <p>
   * Extracts the route {@link String} from {@link SharedPreferences} and converts
   * it back to a {@link DirectionsRoute} object with {@link Gson}.
   *
   * @param context to retrieve {@link SharedPreferences}
   * @return {@link DirectionsRoute} stored when launching
   */
  static DirectionsRoute extractRoute(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String directionsRouteJson = preferences.getString(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY, "");
    return DirectionsRoute.fromJson(directionsRouteJson);
  }

  static void cleanUpPreferences(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor
      .remove(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY)
      .remove(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE)
      .remove(NavigationConstants.NAVIGATION_VIEW_PREFERENCE_SET_THEME)
      .remove(NavigationConstants.NAVIGATION_VIEW_PREFERENCE_SET_THEME)
      .remove(NavigationConstants.NAVIGATION_VIEW_LIGHT_THEME)
      .remove(NavigationConstants.NAVIGATION_VIEW_DARK_THEME)
      .remove(NavigationConstants.OFFLINE_PATH_KEY)
      .remove(NavigationConstants.OFFLINE_VERSION_KEY)
      .remove(NavigationConstants.MAP_DATABASE_PATH_KEY)
      .remove(NavigationConstants.MAP_STYLE_URL_KEY)
      .apply();
  }

  private static void storeDirectionsRouteValue(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    editor.putString(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY, options.directionsRoute().toJson());
  }

  private static void storeConfiguration(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    editor.putBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, options.shouldSimulateRoute());
  }

  private static void storeThemePreferences(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    boolean preferenceThemeSet = options.lightThemeResId() != null || options.darkThemeResId() != null;
    editor.putBoolean(NavigationConstants.NAVIGATION_VIEW_PREFERENCE_SET_THEME, preferenceThemeSet);

    if (preferenceThemeSet) {
      if (options.lightThemeResId() != null) {
        editor.putInt(NavigationConstants.NAVIGATION_VIEW_LIGHT_THEME, options.lightThemeResId());
      }
      if (options.darkThemeResId() != null) {
        editor.putInt(NavigationConstants.NAVIGATION_VIEW_DARK_THEME, options.darkThemeResId());
      }
    }
  }

  private static void storeInitialMapPosition(NavigationLauncherOptions options, Intent navigationActivity) {
    if (options.initialMapCameraPosition() != null) {
      navigationActivity.putExtra(
        NavigationConstants.NAVIGATION_VIEW_INITIAL_MAP_POSITION, options.initialMapCameraPosition()
      );
    }
  }

  private static void storeOfflinePath(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    editor.putString(NavigationConstants.OFFLINE_PATH_KEY, options.offlineRoutingTilesPath());
  }

  private static void storeOfflineVersion(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    editor.putString(NavigationConstants.OFFLINE_VERSION_KEY, options.offlineRoutingTilesVersion());
  }

  private static void storeOfflineMapDatabasePath(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    editor.putString(NavigationConstants.MAP_DATABASE_PATH_KEY, options.offlineMapOptions().getDatabasePath());
  }

  private static void storeOfflineMapStyleUrl(NavigationLauncherOptions options, SharedPreferences.Editor editor) {
    editor.putString(NavigationConstants.MAP_STYLE_URL_KEY, options.offlineMapOptions().getStyleUrl());
  }
}
