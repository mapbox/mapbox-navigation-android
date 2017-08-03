package com.mapbox.services.android.navigation.ui.v5.searchdetailsview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;

public class CustomDividerItemDecorator extends RecyclerView.ItemDecoration {

  private Drawable divider;

  public CustomDividerItemDecorator(Context context) {
    divider = ContextCompat.getDrawable(context, R.drawable.recycler_view_decorator);
  }

  @Override
  public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
    int left = parent.getPaddingLeft();
    int right = parent.getWidth() - parent.getPaddingRight();

    int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++) {
      View child = parent.getChildAt(i);

      RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

      int top = child.getBottom() + params.bottomMargin;
      int bottom = top + divider.getIntrinsicHeight();

      divider.setBounds(left, top, right, bottom);

      divider.draw(canvas);
    }
  }
}