package com.mapbox.navigation.core.utils.routeRefresh

internal class RouteRefreshUtils {

    /**
     * Checks if a refresh result is stale relative to the current primary route.
     *
     * A result is considered stale when the primary route changed between the moment
     * the refresh was requested and the moment its result arrived, identified by a
     * mismatch in the primary route id.
     *
     * If either id is null, the comparison is skipped and the result is treated as
     * not stale, since one side does not carry enough information to make a decision.
     *
     * @param currentPrimaryId id of the primary route at the time the result is processed
     * @param refreshedPrimaryId id of the primary route the refresh was issued for
     * @return true when both ids are non-null and differ, false otherwise
     */
    fun isResultStale(currentPrimaryId: String?, refreshedPrimaryId: String?) =
        if (currentPrimaryId == null || refreshedPrimaryId == null) {
            false
        } else {
            currentPrimaryId != refreshedPrimaryId
        }
}
