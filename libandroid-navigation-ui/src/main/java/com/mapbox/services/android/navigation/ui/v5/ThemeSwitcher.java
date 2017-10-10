package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.maps.MapView;

/**
 * This class is used to switch theme colors in {@link NavigationView}.
 */
public class ThemeSwitcher {

  /**
   * Called in onCreate() to check the UI Mode (day or night)
   * and set the theme colors accordingly.
   *
   * @param context {@link NavigationView} where the theme will be set
   */
  static void setTheme(Context context) {
    int uiMode = context.getResources().getConfiguration().uiMode
      & Configuration.UI_MODE_NIGHT_MASK;
    boolean darkThemeEnabled = uiMode == Configuration.UI_MODE_NIGHT_YES;
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(context.getString(R.string.dark_theme_enabled), darkThemeEnabled);
    editor.apply();
    context.setTheme(darkThemeEnabled ? R.style.NavigationViewDark : R.style.NavigationViewLight);
  }

  /**
   * Can be called to toggle the theme based on the current theme setting.
   *
   * @param activity {@link NavigationView} where the theme will be set
   */
  static void toggleTheme(Activity activity) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean(activity.getString(R.string.dark_theme_enabled), false);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(activity.getString(R.string.dark_theme_enabled), !darkThemeEnabled);
    editor.apply();
    activity.recreate();
  }

  /**
   * Sets the {@link MapView} style based on the current theme setting.
   *
   * @param context to retrieve {@link SharedPreferences}
   * @param mapView  the style will be set on
   */
  static void setMapStyle(Context context, MapView mapView) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean darkThemeEnabled = preferences.getBoolean(context.getString(R.string.dark_theme_enabled), false);
    String nightThemeUrl = context.getString(R.string.navigation_guidance_night_v2);
    String dayThemeUrl = context.getString(R.string.navigation_guidance_day_v2);
    mapView.setStyleUrl(darkThemeEnabled ? nightThemeUrl : dayThemeUrl);
  }

  /**
   * Returns a map marker {@link Icon} based on the current theme setting.
   *
   * @param context to retrieve {@link SharedPreferences} and an instance of {@link IconFactory}
   * @return {@link Icon} map marker dark or light
   */
  static Icon retrieveMapMarker(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean darkThemeEnabled = preferences.getBoolean(context.getString(R.string.dark_theme_enabled), false);
    IconFactory iconFactory = IconFactory.getInstance(context);
    return iconFactory.fromResource(darkThemeEnabled ? R.drawable.map_marker_dark : R.drawable.map_marker_light);
  }

  /**
   * Looks are current theme and retrieves the primary color
   * for the given set theme.
   *
   * @param context to retrieve {@link SharedPreferences} and color with {@link ContextCompat}
   * @return color resource identifier for primary theme color
   */
  public static int retrieveNavigationViewPrimaryColor(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean darkThemeEnabled = preferences.getBoolean(context.getString(R.string.dark_theme_enabled), false);
    TypedArray styleArray = context.obtainStyledAttributes(
      darkThemeEnabled ? R.style.NavigationViewDark : R.style.NavigationViewLight,
      R.styleable.NavigationView
    );
    int navigationViewPrimary = styleArray.getColor(R.styleable.NavigationView_navigationViewPrimary,
      ContextCompat.getColor(context, R.color.mapbox_navigation_view_color_primary));
    styleArray.recycle();
    return navigationViewPrimary;
  }

  /**
   * Looks are current theme and retrieves the secondary color
   * for the given set theme.
   *
   * @param context to retrieve {@link SharedPreferences} and color with {@link ContextCompat}
   * @return color resource identifier for primary theme color
   */
  public static int retrieveNavigationViewSecondaryColor(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean darkThemeEnabled = preferences.getBoolean(context.getString(R.string.dark_theme_enabled), false);
    TypedArray styleArray = context.obtainStyledAttributes(
      darkThemeEnabled ? R.style.NavigationViewDark : R.style.NavigationViewLight,
      R.styleable.NavigationView
    );
    int navigationViewPrimary = styleArray.getColor(R.styleable.NavigationView_navigationViewSecondary,
      ContextCompat.getColor(context, R.color.mapbox_navigation_view_color_secondary));
    styleArray.recycle();
    return navigationViewPrimary;
  }
}