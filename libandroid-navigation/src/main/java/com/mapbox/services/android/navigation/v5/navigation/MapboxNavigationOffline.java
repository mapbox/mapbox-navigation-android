package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import com.mapbox.mapboxsdk.navigation.plugin.OfflinePlugin;
import com.mapbox.navigator.Navigator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapboxNavigationOffline {
    private final List<OfflinePlugin> plugins;
    private final Navigator navigator;

    private MapboxNavigationOffline(@NonNull Builder builder) {
        this.navigator = builder.navigator;
        this.plugins = Collections.unmodifiableList(builder.plugins);
    }

    /**
     * Configures the navigator for offline routes.
     *
     * @param tileSetPath tile set path to configure
     */
    public long configure(@NonNull String tileSetPath) {
        synchronized (this) {
            return navigator.configureRouter(tileSetPath, null, null);
        }
    }

    public static final class Builder {
        private final Navigator navigator;
        private final List<OfflinePlugin> plugins = new ArrayList<>();

        Builder(Navigator navigator) {
            this.navigator = navigator;
        }

        public Builder addPlugin(@NonNull OfflinePlugin plugin) {
            if (plugins.contains(plugin)) {
                return this;
            }
            plugins.add(plugin);
            return this;
        }

        public MapboxNavigationOffline install() {
            return new MapboxNavigationOffline(this);
        }
    }
}
