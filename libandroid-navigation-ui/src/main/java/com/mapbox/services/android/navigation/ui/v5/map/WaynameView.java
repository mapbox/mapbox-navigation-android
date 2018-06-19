package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.CardView;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;

class WaynameView extends CardView {

  private static final int BACKGROUND_ALPHA = 220;

  private TextView waynameText;
  private int waynameHeight;

  WaynameView(Context context) {
    super(context);
    init();
    waynameHeight = (int) context.getResources().getDimension(R.dimen.wayname_view_height);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, waynameHeight);
  }

  void setWaynameText(String waynameText) {
    this.waynameText.setText(waynameText);
  }

  private void init() {
    inflate(getContext(), R.layout.wayname_view_layout, this);
    waynameText = findViewById(R.id.waynameText);
    Drawable waynameTextBackground = waynameText.getBackground();
    initializeBackground(waynameTextBackground);
  }

  private void initializeBackground(Drawable waynameTextBackground) {
    waynameTextBackground.setAlpha(BACKGROUND_ALPHA);
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int navigationViewPrimaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewPrimary);
      Drawable soundChipBackground = DrawableCompat.wrap(waynameTextBackground).mutate();
      DrawableCompat.setTint(soundChipBackground, navigationViewPrimaryColor);
    }
  }
}
