package com.mapbox.services.android.navigation.ui.v5.alert;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

public class AlertView extends LinearLayout {

  private TextView alertText;

  private Animation slideUpTop;
  private Animation slideDownTop;

  public AlertView(Context context) {
    this(context, null);
  }

  public AlertView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public AlertView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    initAnimations();
  }

  public void show(String alertText) {
    this.alertText.setText(alertText);
    if (this.getVisibility() == INVISIBLE) {
      this.setVisibility(VISIBLE);
      this.startAnimation(slideDownTop);
    }
  }

  private void init() {
    inflate(getContext(), R.layout.alert_view_layout, this);
  }

  private void bind() {
    alertText = findViewById(R.id.alertText);
  }

  private void initAnimations() {
    slideUpTop = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_top);
    slideDownTop = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down_top);
  }
}
