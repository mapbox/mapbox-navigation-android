package com.mapbox.navigation.ui.route;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

class MapRouteDrawableProvider {

  private final Context context;

  MapRouteDrawableProvider(Context context) {
    this.context = context;
  }

  @Nullable
  Drawable retrieveDrawable(int resId) {
    return AppCompatResources.getDrawable(context, resId);
  }
}