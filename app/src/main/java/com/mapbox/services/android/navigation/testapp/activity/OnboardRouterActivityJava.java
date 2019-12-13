package com.mapbox.services.android.navigation.testapp.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter;
import com.mapbox.navigation.route.onboard.model.Config;

public class OnboardRouterActivityJava extends AppCompatActivity {

  private Router offboardRouter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setupRouter();
  }

  private void setupRouter() {
    Config config = new Config(
            this.getApplication().getExternalFilesDir(null).getPath(),
            null,
            null,
            null,
            null);

    offboardRouter = new MapboxOnboardRouter(Mapbox.getAccessToken(), config);
  }
}
