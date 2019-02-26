package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;

class CustomNavigationView extends NavigationView {
  CustomNavigationView(Context context) {
    super(context);
  }

  CustomNavigationView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  CustomNavigationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected int getLayoutRes() {
    return R.layout.custom_navigation_view;
  }
}
