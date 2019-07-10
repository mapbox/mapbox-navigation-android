package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Bundle;
import com.mapbox.mapboxsdk.navigation.plugin.DiagnosticsPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapboxNavigationDiagnostics {
    private final List<DiagnosticsPlugin> plugins;
    private final MapboxNavigator navigator;

    private MapboxNavigationDiagnostics(Builder builder) {
        this.navigator = builder.navigator;
        this.plugins = Collections.unmodifiableList(builder.plugins);
    }

    public void enable(boolean enable) {
        navigator.toggleHistory(enable);
    }

    public void addEvent(MapboxNavigationDiagnostics.Event event, Bundle bundle) {
        // TODO: Deserialize bundle and add data to history
        //navigator.addHistoryEvent();
    }

    public void flush() {
        String json = navigator.retrieveHistory();
        // TODO: every plugin call should be executed on separate background thread
        // Perhaps it's best to use thread pool here...
        for (DiagnosticsPlugin plugin: plugins) {
            plugin.onFlush(json);
        }
    }

    public static final class Builder {
        private final MapboxNavigator navigator;
        private final List<DiagnosticsPlugin> plugins = new ArrayList<>();

        Builder(MapboxNavigator navigator) {
            this.navigator = navigator;
        }

        public Builder addPlugin(DiagnosticsPlugin plugin) {
            if (plugins.contains(plugin)) {
                return this;
            }
            plugins.add(plugin);
            return this;
        }

        public MapboxNavigationDiagnostics install() {
            return new MapboxNavigationDiagnostics(this);
        }
    }

    public static final class Event {
        private Event() {}
        public static final String GENERIC_EVENT_TYPE = "generic";
    }
}
