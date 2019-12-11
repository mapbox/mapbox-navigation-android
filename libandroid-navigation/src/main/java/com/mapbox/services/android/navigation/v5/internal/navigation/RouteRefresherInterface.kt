package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import java.util.Date

interface RouteRefresherInterface {
    fun check(currentDate: Date): Boolean
    fun refresh(routeProgress: RouteProgress)
    fun updateLastRefresh(date: Date)
    fun updateIsChecking(isChecking: Boolean)
}
