package com.mapbox.navigation.utils.thread

enum class Priority(val priorityValue: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    IMMEDIATE(3);
}