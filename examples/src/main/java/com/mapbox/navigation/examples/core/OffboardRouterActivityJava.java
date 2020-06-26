package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.route.offboard.internal.MapboxOffboardRouter;

/**
 * Along with {@link BaseRouterActivityJava}, this activity shows how to
 * use the Navigation SDK's
 * {@link MapboxOffboardRouter}.
 */
public class OffboardRouterActivityJava extends BaseRouterActivityJava {

  @Override
  Router setupRouter() {
    return setupOffboardRouter(this);
  }
}
