package com.mapbox.navigation.examples.core;

import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.route.onboard.internal.MapboxOnboardRouter;

/**
 * Along with {@link BaseRouterActivityJava}, this activity shows how to
 * use the Navigation SDK's
 * {@link MapboxOnboardRouter}.
 */
public class OnboardRouterActivityJava extends BaseRouterActivityJava {

  @Override
  Router setupRouter() {
    return setupOnboardRouter(this);
  }
}
