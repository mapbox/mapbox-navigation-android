package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;

public class OnboardRouterActivityJava extends BaseRouterActivityJava {

  @Override
  Router setupRouter() {
    return setupOnboardRouter(this);
  }
}
