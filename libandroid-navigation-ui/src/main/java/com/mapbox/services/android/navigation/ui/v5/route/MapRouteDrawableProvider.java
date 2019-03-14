package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;

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