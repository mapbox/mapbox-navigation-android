package com.mapbox.navigation.ui.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mapbox.navigation.ui.R;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationConstants;

/**
 * This class is used to switch theme colors in {@link NavigationView}.
 */
public class ThemeSwitcher {

  /**
   * Returns a map marker {@link Bitmap} based on the current theme setting.
   *
   * @param context to retrieve the drawable for the given resource ID
   * @return {@link Bitmap} map marker dark or light
   */
  public static Bitmap retrieveThemeMapMarker(Context context) {
    TypedValue destinationMarkerResId = resolveAttributeFromId(context, R.attr.navigationViewDestinationMarker);
    int markerResId = destinationMarkerResId.resourceId;
    if (!isValid(markerResId)) {
      if (isNightModeEnabled(context)) {
        markerResId = R.drawable.map_marker_dark;
      } else {
        markerResId = R.drawable.map_marker_light;
      }
    }

    Drawable markerDrawable = ContextCompat.getDrawable(context, markerResId);
    return BitmapUtils.getBitmapFromDrawable(markerDrawable);
  }

  /**
   * Looks at current theme and retrieves the resource
   * for the given attrId set in the theme.
   *
   * @param context to retrieve the resolved attribute
   * @param attrId  for the given attribute Id
   * @return resolved resource Id
   */
  public static int retrieveAttrResourceId(Context context, int attrId, int defaultResId) {
    TypedValue outValue = resolveAttributeFromId(context, attrId);
    if (isValid(outValue.resourceId)) {
      return outValue.resourceId;
    } else {
      return defaultResId;
    }
  }

  /**
   * Called in onCreate() to check the UI Mode (day or night)
   * and set the theme colors accordingly.
   *
   * @param context {@link NavigationView} where the theme will be set
   * @param attrs   holding custom styles if any are set
   */
  public static void setTheme(Context context, AttributeSet attrs) {
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

  public static String retrieveMapStyle(Context context) {
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

  private static boolean isValid(@AnyRes int resId) {
    return resId != -1 && (resId & 0xff000000) != 0 && (resId & 0x00ff0000) != 0;
  }
}