package com.mapbox.services.android.navigation.v5.utils.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.style.ImageSpan;

import java.lang.ref.WeakReference;

public class CenteredImageSpan extends ImageSpan {

  private WeakReference<Drawable> drawableWeakReference;

  public CenteredImageSpan(Drawable drawable, final int verticalAlignment) {
    super(drawable, verticalAlignment);
  }

  @Override
  public int getSize(Paint paint, CharSequence text,
                     int start, int end,
                     Paint.FontMetricsInt metricsInt) {

    Drawable drawable = getCachedDrawable();
    Rect rect = drawable.getBounds();

    if (metricsInt != null) {
      Paint.FontMetricsInt pfm = paint.getFontMetricsInt();
      metricsInt.ascent = pfm.ascent;
      metricsInt.descent = pfm.descent;
      metricsInt.top = pfm.top;
      metricsInt.bottom = pfm.bottom;
    }

    return rect.right;
  }

  @Override
  public void draw(@NonNull Canvas canvas, CharSequence text,
                   int start, int end, float dx,
                   int top, int dy, int bottom, @NonNull Paint paint) {
    Drawable drawable = getCachedDrawable();
    canvas.save();

    int drawableHeight = drawable.getIntrinsicHeight();
    int fontAscent = paint.getFontMetricsInt().ascent;
    int fontDescent = paint.getFontMetricsInt().descent;
    int transY = bottom - drawable.getBounds().bottom +  // align bottom to bottom
      (drawableHeight - fontDescent + fontAscent) / 2;  // align center to center

    canvas.translate(dx, transY);
    drawable.draw(canvas);
    canvas.restore();
  }

  private Drawable getCachedDrawable() {
    WeakReference<Drawable> reference = drawableWeakReference;
    Drawable drawable = null;

    if (reference != null) {
      drawable = reference.get();
    }

    if (drawable == null) {
      drawable = getDrawable();
      drawableWeakReference = new WeakReference<>(drawable);
    }

    return drawable;
  }
}
