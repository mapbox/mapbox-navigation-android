package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;

public class HybridRouterActivityJava extends BaseRouterActivityJava {

  @Override Router setupRouter() {
    return setupHybridRouter(this);
  }
}
