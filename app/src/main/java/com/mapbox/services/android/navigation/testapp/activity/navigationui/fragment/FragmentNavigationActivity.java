package com.mapbox.services.android.navigation.testapp.activity.navigationui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.services.android.navigation.testapp.R;

public class FragmentNavigationActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_fragment);
  }
}
