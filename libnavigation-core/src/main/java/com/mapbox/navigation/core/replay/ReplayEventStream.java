package com.mapbox.navigation.core.replay;

import com.mapbox.navigation.core.replay.history.ReplayEventBase;
import com.mapbox.navigation.core.replay.history.ReplayHistoryEventStream;
import com.mapbox.navigation.core.replay.route.ReplayRouteOptions;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Interface used for memory efficient replay systems. This is needed for simulations that include
 * many events, for example high frequencies {@link ReplayRouteOptions#getFrequency()}. This is
 * also needed for large history files.
 *
 * @see ReplayHistoryEventStream
 */
public interface ReplayEventStream extends Closeable, Iterator<ReplayEventBase> {

  /**
   * Remove is not supported by default.
   */
  default void remove() {
    throw new UnsupportedOperationException("remove");
  }

  /**
   * Close will do nothing by default.
   */
  default void close() {
    // No operation by default
  }
}
