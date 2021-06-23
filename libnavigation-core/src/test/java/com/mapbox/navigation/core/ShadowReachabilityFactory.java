package com.mapbox.navigation.core;

import androidx.annotation.Nullable;

import com.mapbox.common.ReachabilityFactory;
import com.mapbox.common.ReachabilityInterface;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.mock;

@Implements(ReachabilityFactory.class)
public class ShadowReachabilityFactory {

  @Implementation
  public static ReachabilityInterface reachability(@Nullable String hostname) {
    return mock(ReachabilityInterface.class);
  }
}
