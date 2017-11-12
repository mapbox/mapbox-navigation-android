package com.mapbox.services.android.navigation.v5.utils.distance;

/**
 * Created by selimyildiz on 12/11/2017.
 */

public enum UnitType {
    UNIT_IMPERIAL(0),
    UNIT_METRIC(1);
    private final int id;
    UnitType(int id) {
        this.id = id;
    }
    public int getValue() {
        return id;
    }

}

