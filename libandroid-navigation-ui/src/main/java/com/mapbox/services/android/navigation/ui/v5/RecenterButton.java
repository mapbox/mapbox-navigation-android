package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

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
public class RecenterButton extends CardView implements NavigationButton {
  private MultiOnClickListener multiOnClickListener = new MultiOnClickListener();
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
  @Override
  public void show() {
    if (getVisibility() == INVISIBLE) {
      setVisibility(VISIBLE);
      startAnimation(slideUpBottom);
    }
  }

  /**
   * Adds an onClickListener to the button
   *
   * @param onClickListener to add
   */
  @Override
  public void addOnClickListener(OnClickListener onClickListener) {
    multiOnClickListener.addListener(onClickListener);
  }

  /**
   * Removes an onClickListener from the button
   *
   * @param onClickListener to remove
   */
  @Override
  public void removeOnClickListener(OnClickListener onClickListener) {
    multiOnClickListener.removeListener(onClickListener);
  }

  /**
   * Sets visibility to INVISIBLE.
   *
   * @since 0.6.0
   */
  @Override
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

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    setupOnClickListeners();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    clearListeners();
  }

  private void setupOnClickListeners() {
    setOnClickListener(multiOnClickListener);
  }

  private void clearListeners() {
    multiOnClickListener.clearListeners();
    multiOnClickListener = null;
    setOnClickListener(null);
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
