package com.mapbox.navigation.ui.internal.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Base64;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.io.ByteArrayOutputStream;

public class ViewUtils {

  public static Bitmap captureView(@NonNull View view) {
    View rootView = view.getRootView();
    rootView.setDrawingCacheEnabled(true);
    Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
    rootView.setDrawingCacheEnabled(false);
    return bitmap;
  }

  public static String encodeView(@NonNull Bitmap capture) {
    return encodeView(capture, new BitmapEncodeOptions.Builder().build());
  }

  public static String encodeView(@NonNull Bitmap capture, @NonNull BitmapEncodeOptions options) {
    // Resize up to original width while keeping the aspect ratio
    int width = Math.min(capture.getWidth(), options.getWidth());
    int height = Math.round((float) width * capture.getHeight() / capture.getWidth());
    Bitmap scaled = Bitmap.createScaledBitmap(capture, width, height, /*filter=*/true);

    // Convert to JPEG at a quality between 20% ~ 100%
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    scaled.compress(Bitmap.CompressFormat.JPEG, options.getCompressQuality(), stream);

    // Convert to base64 encoded string
    byte[] data = stream.toByteArray();
    return Base64.encodeToString(data, Base64.DEFAULT);
  }

  @Nullable
  public static Bitmap decodeScreenshot(String screenshotBase64Format) {
    try {
      byte[] bytes = Base64.decode(screenshotBase64Format, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    } catch (Exception exception) {
      return null;
    }
  }

  @Nullable
  public static Bitmap loadBitmapFromView(@NonNull View view) {
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

  public static boolean isLandscape(@NonNull Context context) {
    return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
  }
}
