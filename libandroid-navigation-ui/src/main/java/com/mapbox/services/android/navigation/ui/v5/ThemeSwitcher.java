package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

/**
 * This class is used to switch theme colors in {@link NavigationView}.
 */
public class ThemeSwitcher {

  /**
   * Looks are current theme and retrieves the color attribute
   * for the given set theme.
   *
   * @param context to retrieve the set theme and resolved attribute and then color res Id with {@link ContextCompat}
   * @return color resource identifier for primary theme color
   */
  public static int retrieveNavigationViewThemeColor(Context context, int resId) {
    TypedValue outValue = obtainTypedValue(context, resId);
    if (outValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
      && outValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
      return outValue.data;
    } else {
      return ContextCompat.getColor(context, outValue.resourceId);
    }
  }

  /**
   * Called in onCreate() to check the UI Mode (day or night)
   * and set the theme colors accordingly.
   *
   * @param context {@link NavigationView} where the theme will be set
   * @param attrs   holding custom styles if any are set
   */
  static void setTheme(Context context, AttributeSet attrs) {
    int uiMode = context.getResources().getConfiguration().uiMode
      & Configuration.UI_MODE_NIGHT_MASK;
    boolean darkThemeEnabled = uiMode == Configuration.UI_MODE_NIGHT_YES;
    updatePreferencesDarkEnabled(context, darkThemeEnabled);

    // Check for custom theme from NavigationLauncher
    if (shouldSetThemeFromPreferences(context)) {
      int prefLightTheme = retrieveThemeResIdFromPreferences(context, NavigationConstants.NAVIGATION_VIEW_LIGHT_THEME);
      int prefDarkTheme = retrieveThemeResIdFromPreferences(context, NavigationConstants.NAVIGATION_VIEW_DARK_THEME);
      prefLightTheme = prefLightTheme == 0 ?  R.style.NavigationViewLight : prefLightTheme;
      prefDarkTheme = prefLightTheme == 0 ?  R.style.NavigationViewDark : prefDarkTheme;
      context.setTheme(darkThemeEnabled ? prefDarkTheme : prefLightTheme);
      return;
    }

    TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.NavigationView);
    int lightTheme = styledAttributes.getResourceId(R.styleable.NavigationView_navigationLightTheme,
      R.style.NavigationViewLight);
    int darkTheme = styledAttributes.getResourceId(R.styleable.NavigationView_navigationDarkTheme,
      R.style.NavigationViewDark);
    styledAttributes.recycle();

    context.setTheme(darkThemeEnabled ? darkTheme : lightTheme);
  }

  /**
   * Sets the {@link MapView} style based on the current theme setting.
   *
   * @param context to retrieve {@link SharedPreferences}
   * @param map     the style will be set on
   */
  static void setMapStyle(Context context, MapboxMap map, MapboxMap.OnStyleLoadedListener listener) {
    TypedValue mapStyleAttr = obtainTypedValue(context, R.attr.navigationViewMapStyle);
    String styleUrl = mapStyleAttr.string.toString();
    map.setStyleUrl(styleUrl, listener);
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
   * Looks are current theme and retrieves the style
   * for the given resId set in the theme.
   *
   * @param context to retrieve the resolved attribute
   * @param styleResId for the given style
   * @return resolved style resource Id
   */
  static int retrieveNavigationViewStyle(Context context, int styleResId) {
    TypedValue outValue = obtainTypedValue(context, styleResId);
    return outValue.resourceId;
  }

  @NonNull
  private static TypedValue obtainTypedValue(Context context, int resId) {
    TypedValue outValue = new TypedValue();
    context.getTheme().resolveAttribute(resId, outValue, true);
    return outValue;
  }

  private static void updatePreferencesDarkEnabled(Context context, boolean darkThemeEnabled) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(context.getString(R.string.dark_theme_enabled), darkThemeEnabled);
    editor.apply();
  }

  private static boolean shouldSetThemeFromPreferences(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getBoolean(NavigationConstants.NAVIGATION_VIEW_PREFERENCE_SET_THEME, false);
  }

  private static int retrieveThemeResIdFromPreferences(Context context, String key) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getInt(key, 0);
  }
}