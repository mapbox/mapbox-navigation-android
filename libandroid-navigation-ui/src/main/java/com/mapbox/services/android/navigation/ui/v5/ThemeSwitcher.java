package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.mapbox.mapboxsdk.utils.BitmapUtils;
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
  public static int retrieveThemeColor(Context context, int resId) {
    TypedValue outValue = resolveAttributeFromId(context, resId);
    if (outValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
      && outValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
      return outValue.data;
    } else {
      return ContextCompat.getColor(context, outValue.resourceId);
    }
  }

  /**
   * Returns a map marker {@link Bitmap} based on the current theme setting.
   *
   * @param context to retrieve the drawable for the given resource ID
   * @return {@link Bitmap} map marker dark or light
   */
  public static Bitmap retrieveThemeMapMarker(Context context) {
    TypedValue destinationMarkerResId = resolveAttributeFromId(context, R.attr.navigationViewDestinationMarker);
    int markerResId = destinationMarkerResId.resourceId;
    Drawable markerDrawable = ContextCompat.getDrawable(context, markerResId);
    return BitmapUtils.getBitmapFromDrawable(markerDrawable);
  }

  /**
   * Returns a route overview {@link Drawable} based on the current theme setting.
   *
   * @param context to retrieve overview {@link Drawable}
   * @return {@link Drawable} for route overview - dark or light
   */
  public static Drawable retrieveThemeOverviewDrawable(Context context) {
    TypedValue destinationMarkerResId = resolveAttributeFromId(context, R.attr.navigationViewRouteOverviewDrawable);
    int overviewResId = destinationMarkerResId.resourceId;
    return AppCompatResources.getDrawable(context, overviewResId);
  }

  /**
   * Looks are current theme and retrieves the style
   * for the given resId set in the theme.
   *
   * @param context    to retrieve the resolved attribute
   * @param styleResId for the given style
   * @return resolved style resource Id
   */
  public static int retrieveNavigationViewStyle(Context context, int styleResId) {
    TypedValue outValue = resolveAttributeFromId(context, styleResId);
    return outValue.resourceId;
  }

  /**
   * Called in onCreate() to check the UI Mode (day or night)
   * and set the theme colors accordingly.
   *
   * @param context {@link NavigationView} where the theme will be set
   * @param attrs   holding custom styles if any are set
   */
  static void setTheme(Context context, AttributeSet attrs) {
    boolean nightModeEnabled = isNightModeEnabled(context);

    if (shouldSetThemeFromPreferences(context)) {
      int prefLightTheme = retrieveThemeResIdFromPreferences(context, NavigationConstants.NAVIGATION_VIEW_LIGHT_THEME);
      int prefDarkTheme = retrieveThemeResIdFromPreferences(context, NavigationConstants.NAVIGATION_VIEW_DARK_THEME);
      prefLightTheme = prefLightTheme == 0 ? R.style.NavigationViewLight : prefLightTheme;
      prefDarkTheme = prefLightTheme == 0 ? R.style.NavigationViewDark : prefDarkTheme;
      context.setTheme(nightModeEnabled ? prefDarkTheme : prefLightTheme);
      return;
    }

    TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.NavigationView);
    int lightTheme = styledAttributes.getResourceId(R.styleable.NavigationView_navigationLightTheme,
      R.style.NavigationViewLight);
    int darkTheme = styledAttributes.getResourceId(R.styleable.NavigationView_navigationDarkTheme,
      R.style.NavigationViewDark);
    styledAttributes.recycle();

    context.setTheme(nightModeEnabled ? darkTheme : lightTheme);
  }

  static String retrieveMapStyle(Context context) {
    TypedValue mapStyleAttr = resolveAttributeFromId(context, R.attr.navigationViewMapStyle);
    return mapStyleAttr.string.toString();
  }

  /**
   * Returns true if the current UI_MODE_NIGHT is enabled, false otherwise.
   */
  private static boolean isNightModeEnabled(Context context) {
    int currentNightMode = retrieveCurrentUiMode(context);
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
  }

  private static int retrieveCurrentUiMode(Context context) {
    return context.getResources().getConfiguration().uiMode
      & Configuration.UI_MODE_NIGHT_MASK;
  }

  @NonNull
  private static TypedValue resolveAttributeFromId(Context context, int resId) {
    TypedValue outValue = new TypedValue();
    context.getTheme().resolveAttribute(resId, outValue, true);
    return outValue;
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