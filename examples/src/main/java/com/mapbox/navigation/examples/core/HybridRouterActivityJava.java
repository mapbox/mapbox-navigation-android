package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;

/**
 * Along with {@link BaseRouterActivityJava}, this activity shows how to
 * use the Navigation SDK's
 * {@link com.mapbox.navigation.route.hybrid.MapboxHybridRouter}.
 */
public class HybridRouterActivityJava extends BaseRouterActivityJava {

  @Override Router setupRouter() {
    return setupHybridRouter(getApplicationContext());
  }
}
