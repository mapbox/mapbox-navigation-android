package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;

/**
 * Button used to re-activate following user location during navigation.
 * <p>
 * If a user scrolls the map while navigating, the
 * {@link com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet}
 * is set to hidden and this button is shown.
 * <p>
 * This button uses a custom {@link TranslateAnimation} with {@link OvershootInterpolator}
 * to be shown.
 *
 * @since 0.6.0
 */
public class RecenterButton extends CardView {

  private Animation slideUpBottom;

  public RecenterButton(Context context) {
    this(context, null);
  }

  public RecenterButton(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public RecenterButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  /**
   * Sets visibility to VISIBLE and starts custom animation.
   *
   * @since 0.6.0
   */
  public void show() {
    if (getVisibility() == INVISIBLE) {
      setVisibility(VISIBLE);
      startAnimation(slideUpBottom);
    }
  }

  /**
   * Sets visibility to INVISIBLE.
   *
   * @since 0.6.0
   */
  public void hide() {
    if (getVisibility() == VISIBLE) {
      setVisibility(INVISIBLE);
    }
  }

  /**
   * Once inflation of the view has finished,
   * create the custom animation.
   */
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    initAnimation();
  }

  /**
   * Inflates the layout.
   */
  private void init() {
    inflate(getContext(), R.layout.recenter_btn_layout, this);
  }

  /**
   * Creates the custom animation used to show this button.
   */
  private void initAnimation() {
    slideUpBottom = new TranslateAnimation(0f, 0f, 125f, 0f);
    slideUpBottom.setDuration(300);
    slideUpBottom.setInterpolator(new OvershootInterpolator(2.0f));
  }
}
