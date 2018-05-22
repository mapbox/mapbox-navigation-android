package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;
import android.graphics.Bitmap;

import com.mapbox.services.android.navigation.ui.v5.utils.ViewUtils;

class WaynameLayoutProvider {

  private Context context;

  WaynameLayoutProvider(Context context) {
    this.context = context;
  }

  Bitmap generateLayoutBitmap(String wayname) {
    WaynameView waynameView = new WaynameView(context);
    waynameView.setWaynameText(wayname);
    return ViewUtils.loadBitmapFromView(waynameView);
  }
}
