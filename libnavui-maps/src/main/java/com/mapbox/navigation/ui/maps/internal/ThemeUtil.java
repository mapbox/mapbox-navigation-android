package com.mapbox.navigation.ui.maps.internal;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;

public class ThemeUtil {

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
  private static TypedValue resolveAttributeFromId(@NonNull Context context, int resId) {
    TypedValue outValue = new TypedValue();
    context.getTheme().resolveAttribute(resId, outValue, true);
    return outValue;
  }

  private static boolean isValid(@AnyRes int resId) {
    return resId != -1 && (resId & 0xff000000) != 0 && (resId & 0x00ff0000) != 0;
  }
}