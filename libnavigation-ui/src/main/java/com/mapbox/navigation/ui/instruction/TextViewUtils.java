package com.mapbox.navigation.ui.instruction;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

class TextViewUtils {

  boolean textFits(@NonNull TextView textView, String text) {
    Paint paint = new Paint(textView.getPaint());
    float width = paint.measureText(text);

    if (textView.getMaxWidth() != Integer.MAX_VALUE) {
      return width < textView.getMaxWidth();
    } else {
      return width < textView.getWidth();
    }
  }

  @NonNull
  Drawable createDrawable(@NonNull TextView textView, @NonNull Bitmap bitmap) {
    Drawable drawable = new BitmapDrawable(textView.getContext().getResources(), bitmap);
    int bottom = textView.getLineHeight();
    int right = bottom * bitmap.getWidth() / bitmap.getHeight();
    drawable.setBounds(0, 0, right, bottom);

    return drawable;
  }

  void setImageSpan(@NonNull TextView textView, @NonNull View view, int start, int end) {
    Bitmap bitmap = createBitmapFromView(view);
    setImageSpan(textView, bitmap, start, end);
  }

  private void setImageSpan(@NonNull TextView textView, @NonNull Bitmap bitmap, int start, int end) {
    Drawable drawable = createDrawable(textView, bitmap);
    setImageSpan(textView, drawable, start, end);
  }

  private void setImageSpan(@NonNull TextView textView, @NonNull Drawable drawable, int start, int end) {
    Spannable instructionSpannable = new SpannableString(textView.getText());

    instructionSpannable.setSpan(new ImageSpan(drawable),
      start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    textView.setText(instructionSpannable);
  }

  private Bitmap createBitmapFromView(@NonNull View view) {
    int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    view.measure(measureSpec, measureSpec);

    int measuredWidth = view.getMeasuredWidth();
    int measuredHeight = view.getMeasuredHeight();

    view.layout(0, 0, measuredWidth, measuredHeight);
    Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.TRANSPARENT);
    Canvas canvas = new Canvas(bitmap);
    view.draw(canvas);
    return bitmap;
  }
}
