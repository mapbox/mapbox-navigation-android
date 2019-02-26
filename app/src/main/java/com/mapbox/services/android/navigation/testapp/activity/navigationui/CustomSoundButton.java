package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.SoundButton;

public class CustomSoundButton extends FrameLayout implements SoundButton {
  FloatingActionButton floatingActionButton;
  TextView textView;
  boolean isMuted;


  public CustomSoundButton(Context context) {
    this(context, null);
  }

  public CustomSoundButton(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public CustomSoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context);
  }

  private void initialize(Context context) {
    inflate(context, R.layout.custom_sound_button, this);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
  }

  private void bind() {
    floatingActionButton = findViewById(R.id.button);
    textView = findViewById(R.id.text);
  }

  /**
   * Toggles the muted state.
   *
   * @return whether it is muted or not
   */
  @Override
  public boolean toggleMute() {
    return isMuted ? unmute() : mute();
  }

  private boolean mute() {
    isMuted = true;
    showText("unmute");
    return isMuted;
  }

  private boolean unmute() {
    isMuted = false;
    showText("mute");
    return isMuted;
  }

  private void showText(String text) {
    textView.setText(text);
  }

  @Override
  public void setMuted(boolean muted) {
    this.isMuted = muted;
    if (muted) {
      showText("unmute");
    } else {
      showText("mute");
    }
  }

  @Override
  public boolean isMuted() {
    return isMuted;
  }

  @Override
  public void addOnClickListener(View.OnClickListener onClickListener) {
    floatingActionButton.setOnClickListener(onClickListener);
  }

  @Override
  public void removeOnClickListener(OnClickListener onClickListener) {

  }

  @Override
  public void hide() {
    floatingActionButton.hide();
    textView.setVisibility(GONE);
  }

  @Override
  public void show() {
    floatingActionButton.show();
    textView.setVisibility(VISIBLE);
  }
}
