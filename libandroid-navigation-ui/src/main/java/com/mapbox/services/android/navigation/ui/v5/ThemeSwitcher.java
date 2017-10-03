package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.maps.MapView;

public class ThemeSwitcher {

  static void setTheme(Activity activity) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean(activity.getString(R.string.dark_theme_enabled), false);
    activity.setTheme(darkThemeEnabled ? R.style.NavigationViewDark : R.style.NavigationViewLight);
  }

  static void toggleTheme(Activity activity) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean(activity.getString(R.string.dark_theme_enabled), false);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(activity.getString(R.string.dark_theme_enabled), !darkThemeEnabled);
    editor.apply();
    activity.recreate();
  }

  static void setMapStyle(Activity activity, MapView mapView) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean(activity.getString(R.string.dark_theme_enabled), false);
    String nightThemeUrl = activity.getString(R.string.navigation_guidance_night_v2);
    String dayThemeUrl = activity.getString(R.string.navigation_guidance_day_v2);
    mapView.setStyleUrl(darkThemeEnabled ? nightThemeUrl : dayThemeUrl);
  }

  static Icon retrieveMapMarker(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean darkThemeEnabled = preferences.getBoolean(context.getString(R.string.dark_theme_enabled), false);
    IconFactory iconFactory = IconFactory.getInstance(context);
    return iconFactory.fromResource(darkThemeEnabled ? R.drawable.map_marker_dark : R.drawable.map_marker_light);
  }

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