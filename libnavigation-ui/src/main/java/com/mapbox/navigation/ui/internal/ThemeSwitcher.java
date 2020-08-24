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
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.navigation.ui.NavigationConstants;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.R;

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
  @Nullable
  public static Bitmap retrieveThemeMapMarker(@NonNull Context context) {
    TypedValue destinationMarkerResId = resolveAttributeFromId(context, R.attr.navigationViewDestinationMarker);
    int markerResId = destinationMarkerResId.resourceId;
    if (!isValid(markerResId)) {
      if (isNightModeEnabled(context)) {
        markerResId = R.drawable.mapbox_ic_map_marker_dark;
      } else {
        markerResId = R.drawable.mapbox_ic_map_marker_light;
      }
    }

    Drawable markerDrawable = AppCompatResources.getDrawable(context, markerResId);
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
  public static int retrieveAttrResourceId(@NonNull Context context, int attrId, int defaultResId) {
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
  public static void setTheme(@NonNull Context context, AttributeSet attrs) {
    boolean nightModeEnabled = isNightModeEnabled(context);

    if (shouldSetThemeFromPreferences(context)) {
      int prefLightTheme = retrieveThemeResIdFromPreferences(context, NavigationConstants.NAVIGATION_VIEW_LIGHT_THEME);
      int prefDarkTheme = retrieveThemeResIdFromPreferences(context, NavigationConstants.NAVIGATION_VIEW_DARK_THEME);
      prefLightTheme = prefLightTheme == 0 ? R.style.MapboxStyleNavigationViewLight : prefLightTheme;
      prefDarkTheme = prefLightTheme == 0 ? R.style.MapboxStyleNavigationViewDark : prefDarkTheme;
      context.setTheme(nightModeEnabled ? prefDarkTheme : prefLightTheme);
      return;
    }

    TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.MapboxStyleNavigationView);
    int lightTheme = styledAttributes.getResourceId(R.styleable.MapboxStyleNavigationView_navigationLightTheme,
      R.style.MapboxStyleNavigationViewLight);
    int darkTheme = styledAttributes.getResourceId(R.styleable.MapboxStyleNavigationView_navigationDarkTheme,
      R.style.MapboxStyleNavigationViewDark);
    styledAttributes.recycle();

    context.setTheme(nightModeEnabled ? darkTheme : lightTheme);
  }

  @NonNull
  public static String retrieveMapStyle(@NonNull Context context) {
    TypedValue mapStyleAttr = resolveAttributeFromId(context, R.attr.navigationViewMapStyle);
    return mapStyleAttr.string.toString();
  }

  /**
   * Returns true if the current UI_MODE_NIGHT is enabled, false otherwise.
   */
  private static boolean isNightModeEnabled(@NonNull Context context) {
    int currentNightMode = retrieveCurrentUiMode(context);
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
  }

  private static int retrieveCurrentUiMode(@NonNull Context context) {
    return context.getResources().getConfiguration().uiMode
      & Configuration.UI_MODE_NIGHT_MASK;
  }

  @NonNull
  private static TypedValue resolveAttributeFromId(@NonNull Context context, int resId) {
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