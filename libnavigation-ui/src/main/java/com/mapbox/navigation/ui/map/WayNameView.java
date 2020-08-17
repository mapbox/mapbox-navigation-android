package com.mapbox.navigation.ui.map;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mapbox.navigation.ui.R;

/**
 * View that displays the current road name below the user location icon.
 * @see NavigationMapboxMap#updateWaynameQueryMap(boolean)
 */
public class WayNameView extends FrameLayout {

  private TextView wayNameText;

  private int primaryColor;
  private int secondaryColor;

  public WayNameView(@NonNull Context context) {
    this(context, null);
  }

  public WayNameView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public WayNameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initAttributes(attrs);
    initialize();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    wayNameText = findViewById(R.id.waynameText);
    wayNameText.setTextColor(secondaryColor);

    Drawable waynameTextBackground = wayNameText.getBackground();
    Drawable soundChipBackground = DrawableCompat.wrap(waynameTextBackground).mutate();
    DrawableCompat.setTint(soundChipBackground, primaryColor);
  }

  public void updateWayNameText(String wayNameText) {
    this.wayNameText.setText(wayNameText);
  }

  @NonNull
  public String retrieveWayNameText() {
    return wayNameText.getText().toString();
  }

  public void updateVisibility(boolean isVisible) {
    int visibility = isVisible ? VISIBLE : INVISIBLE;
    if (getVisibility() != visibility) {
      setVisibility(visibility);
    }
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.MapboxStyleWayNameView);
    primaryColor = ContextCompat.getColor(getContext(),
      typedArray.getResourceId(R.styleable.MapboxStyleWayNameView_wayNameViewPrimaryColor,
        R.color.mapbox_way_name_view_primary));
    secondaryColor = ContextCompat.getColor(getContext(),
      typedArray.getResourceId(R.styleable.MapboxStyleWayNameView_wayNameViewSecondaryColor,
        R.color.mapbox_way_name_view_secondary));

    typedArray.recycle();
  }

  private void initialize() {
    inflate(getContext(), R.layout.mapbox_wayname_view, this);
  }
}
