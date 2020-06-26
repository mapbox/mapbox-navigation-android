package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.route.hybrid.internal.MapboxHybridRouter;

/**
 * Along with {@link BaseRouterActivityJava}, this activity shows how to
 * use the Navigation SDK's
 * {@link MapboxHybridRouter}.
 */
public class HybridRouterActivityJava extends BaseRouterActivityJava {

  @Override Router setupRouter() {
    return setupHybridRouter(getApplicationContext());
  }
}
