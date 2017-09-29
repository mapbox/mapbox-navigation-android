package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mapbox.mapboxsdk.maps.MapView;

class ThemeSwitcher {

  static void setTheme(Activity activity) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean("dark_theme_enabled", false);
    activity.setTheme(darkThemeEnabled ? R.style.NavigationViewDark : R.style.NavigationViewLight);
  }

  static void toggleTheme(Activity activity) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean("dark_theme_enabled", false);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean("dark_theme_enabled", !darkThemeEnabled);
    editor.apply();
    activity.recreate();
  }

  static void setMapStyle(Activity activity, MapView mapView) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean darkThemeEnabled = preferences.getBoolean("dark_theme_enabled", false);
    String nightThemeUrl = activity.getString(R.string.navigation_guidance_night_v2);
    String dayThemeUrl = activity.getString(R.string.navigation_guidance_day_v2);
    mapView.setStyleUrl(darkThemeEnabled ? nightThemeUrl : dayThemeUrl);
  }
}
