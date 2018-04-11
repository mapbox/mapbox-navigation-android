package com.mapbox.services.android.navigation.ui.v5.wayname;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

public class WaynameView extends CardView {

  private static final int BACKGROUND_ALPHA = 220;

  private TextView waynameText;

  public WaynameView(Context context) {
    super(context);
    init();
  }

  public WaynameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public WaynameView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  public void setWaynameText(String waynameText) {
    this.waynameText.setText(waynameText);
  }

  private void init() {
    inflate(getContext(), R.layout.wayname_view_layout, this);
    waynameText = findViewById(R.id.waynameText);
    waynameText.getBackground().setAlpha(BACKGROUND_ALPHA);
  }
}
