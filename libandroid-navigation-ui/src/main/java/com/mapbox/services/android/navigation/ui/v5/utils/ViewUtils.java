package com.mapbox.services.android.navigation.ui.v5.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.io.ByteArrayOutputStream;

public class ViewUtils {

  public static Bitmap captureView(View view) {
    View rootView = view.getRootView();
    rootView.setDrawingCacheEnabled(true);
    Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
    rootView.setDrawingCacheEnabled(false);
    return bitmap;
  }

  public static String encodeView(Bitmap capture) {
    // Resize to 250px wide while keeping the aspect ratio
    int width = 250;
    int height = Math.round((float) width * capture.getHeight() / capture.getWidth());
    Bitmap scaled = Bitmap.createScaledBitmap(capture, width, height, /*filter=*/true);

    // Convert to JPEG low-quality (~20%)
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    scaled.compress(Bitmap.CompressFormat.JPEG, 20, stream);

    // Convert to base64 encoded string
    byte[] data = stream.toByteArray();
    return Base64.encodeToString(data, Base64.DEFAULT);
  }

  public static Bitmap loadBitmapFromView(View view) {
    if (view.getMeasuredHeight() <= 0) {
      view.measure(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
      Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
      view.draw(canvas);
      return bitmap;
    }
    return null;
  }
}
