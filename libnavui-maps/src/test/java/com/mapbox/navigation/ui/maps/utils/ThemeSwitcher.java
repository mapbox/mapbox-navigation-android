package com.mapbox.navigation.ui.maps.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import com.mapbox.navigation.ui.maps.R;

/**
 * This class is used to switch theme colors in {@link NavigationView}.
 */
public class ThemeSwitcher {

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
