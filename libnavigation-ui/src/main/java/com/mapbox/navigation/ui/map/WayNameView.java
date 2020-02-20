package com.mapbox.navigation.ui.map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.ui.ThemeSwitcher;

public class WayNameView extends FrameLayout {

  private TextView wayNameText;

  public WayNameView(Context context) {
    super(context);
    initialize();
  }

  public WayNameView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public WayNameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public void updateWayNameText(String wayNameText) {
    this.wayNameText.setText(wayNameText);
  }

  public String retrieveWayNameText() {
    return wayNameText.getText().toString();
  }

  public void updateVisibility(boolean isVisible) {
    int visibility = isVisible ? VISIBLE : INVISIBLE;
    if (getVisibility() != visibility) {
      setVisibility(visibility);
    }
  }

  private void initialize() {
    inflate(getContext(), R.layout.wayname_view_layout, this);
    wayNameText = findViewById(R.id.waynameText);
    Drawable waynameTextBackground = wayNameText.getBackground();
    initializeBackground(waynameTextBackground);
  }

  private void initializeBackground(Drawable waynameTextBackground) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int navigationViewPrimaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewPrimary);
      Drawable soundChipBackground = DrawableCompat.wrap(waynameTextBackground).mutate();
      DrawableCompat.setTint(soundChipBackground, navigationViewPrimaryColor);
    }
  }
}
