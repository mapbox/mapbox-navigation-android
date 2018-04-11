package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;
import android.graphics.Bitmap;

import com.mapbox.services.android.navigation.ui.v5.utils.ViewUtils;
import com.mapbox.services.android.navigation.ui.v5.wayname.WaynameView;

public class MapWaynameLayoutProvider {

  private static final int THIRTY_TWO_DP = 32;
  private Context context;

  MapWaynameLayoutProvider(Context context) {
    this.context = context;
  }

  public Bitmap generateLayoutBitmap(String wayname) {
    WaynameView waynameView = new WaynameView(context);
    waynameView.setWaynameText(wayname);
    return ViewUtils.loadBitmapFromView(waynameView);
  }

  public int retrieveHeight() {
    return (int) ViewUtils.dpToPx(context, THIRTY_TWO_DP);
  }
}
