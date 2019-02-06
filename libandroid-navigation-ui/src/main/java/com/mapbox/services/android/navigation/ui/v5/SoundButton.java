package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

public class SoundButton extends ConstraintLayout implements NavigationButton {
  private static final float ALPHA_VALUE_ZERO = 0;
  private static final float ALPHA_VALUE_ONE = 1;
  private static final long ANIMATION_DURATION_THREE_HUNDRED_MILLIS = 300;
  private static final long ANIMATION_DURATION_ONE_THOUSAND_MILLIS = 1000;

  private FloatingActionButton soundFab;
  private TextView soundChipText;
  private AnimationSet fadeInSlowOut;
  private boolean isMuted;
  private MultiOnClickListener multiOnClickListener = new MultiOnClickListener();

  public SoundButton(Context context) {
    this(context, null);
  }

  public SoundButton(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public SoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context);
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
   * Hides the widget
   */
  @Override
  public void hide() {
    setVisibility(GONE);
  }

  /**
   * Shows the widget
   */
  @Override
  public void show() {
    setVisibility(VISIBLE);
  }

  /**
   * Will toggle the view between muted and unmuted states.
   *
   * @return boolean true if muted, false if not
   * @since 0.6.0
   */
  public boolean toggleMute() {
    return isMuted ? unmute() : mute();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    setupColors();
    initializeAnimation();
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

  /**
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is off.
   */
  void soundFabOff() {
    soundFab.setImageResource(R.drawable.ic_sound_off);
  }

  private void setupOnClickListeners() {
    setOnClickListener(multiOnClickListener);
  }

  private void clearListeners() {
    multiOnClickListener.clearListeners();
    multiOnClickListener = null;
    setOnClickListener(null);
  }

  private void initializeAnimation() {
    Animation fadeIn = new AlphaAnimation(ALPHA_VALUE_ZERO, ALPHA_VALUE_ONE);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(ANIMATION_DURATION_THREE_HUNDRED_MILLIS);

    Animation fadeOut = new AlphaAnimation(ALPHA_VALUE_ONE, ALPHA_VALUE_ZERO);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setStartOffset(ANIMATION_DURATION_ONE_THOUSAND_MILLIS);
    fadeOut.setDuration(ANIMATION_DURATION_ONE_THOUSAND_MILLIS);

    fadeInSlowOut = new AnimationSet(false);
    fadeInSlowOut.addAnimation(fadeIn);
    fadeInSlowOut.addAnimation(fadeOut);
  }

  private void setupColors() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      Drawable soundChipBackground = DrawableCompat.wrap(soundChipText.getBackground()).mutate();
      int navigationViewPrimaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewPrimary);
      DrawableCompat.setTint(soundChipBackground, navigationViewPrimaryColor);
    }
  }

  /**
   * Sets up mute UI event.
   * <p>
   * Shows chip with "Muted" text.
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is off.
   * <p>
   * Sets private state variable to true (muted)
   *
   * @return true, view is in muted state
   */
  private boolean mute() {
    isMuted = true;
    setSoundChipText(getContext().getString(R.string.muted));
    showSoundChip();
    soundFabOff();
    return isMuted;
  }

  /**
   * Sets up unmuted UI event.
   * <p>
   * Shows chip with "Unmuted" text.
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is on.
   * <p>
   * Sets private state variable to false (unmuted)
   *
   * @return false, view is in unmuted state
   */
  private boolean unmute() {
    isMuted = false;
    setSoundChipText(getContext().getString(R.string.unmuted));
    showSoundChip();
    soundFabOn();
    return isMuted;
  }

  /**
   * Sets {@link TextView} inside of chip view.
   *
   * @param text to be displayed in chip view ("Muted"/"Umuted")
   */
  private void setSoundChipText(String text) {
    soundChipText.setText(text);
  }

  /**
   * Shows and then hides the sound chip using {@link AnimationSet}
   */
  private void showSoundChip() {
    soundChipText.startAnimation(fadeInSlowOut);
  }

  private void bind() {
    soundFab = findViewById(R.id.soundFab);
    soundChipText = findViewById(R.id.soundText);
  }

  private void initialize(Context context) {
    inflate(context, R.layout.sound_layout, this);
  }

  /**
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is on.
   */
  private void soundFabOn() {
    soundFab.setImageResource(R.drawable.ic_sound_on);
  }
}
