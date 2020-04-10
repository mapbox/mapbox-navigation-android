package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;

public class OffboardRouterActivityJava extends BaseRouterActivityJava {

  @Override
  Router setupRouter() {
    return setupOffboardRouter(this);
  }
}
