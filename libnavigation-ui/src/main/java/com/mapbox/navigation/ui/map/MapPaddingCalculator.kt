package com.mapbox.navigation.ui.map

import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.navigation.ui.R

internal object MapPaddingCalculator {

    /**
     * Calculates the default padding for the puck taking into account the screen orientation,
     * the size of the bottom sheet and the wayname layout.
     *
     * @param mapView a reference to the MapView
     *
     * @return an array of integers representing the padding that was calculated.
     */
    @JvmStatic
    fun calculateDefaultPadding(mapView: MapView): IntArray {
        val defaultTopPadding = calculateTopPaddingWithoutWayname(mapView)
        val resources = mapView.context.resources
        val waynameLayoutHeight = resources.getDimension(R.dimen.mapbox_wayname_view_height).toInt()
        val topPadding =
            defaultTopPadding - waynameLayoutHeight * MapPaddingAdjustor.WAYNAME_PADDING_MULTIPLIER
        return intArrayOf(0, topPadding, 0, 0)
    }

    /**
     * Calculates the default padding for the puck taking into account the screen orientation
     * and the size of the bottom sheet.
     *
     * @param mapView a reference to the MapView
     *
     * @return the top padding value that was calculated
     */
    @JvmStatic
    fun calculateTopPaddingWithoutWayname(mapView: MapView): Int {
        val context = mapView.context
        val bottomSheetHeight =
            context.resources.getDimension(R.dimen.mapbox_summary_bottom_sheet_height).toInt()

        return if (mapView.height > mapView.width) {
            mapView.height
                .minus(
                    bottomSheetHeight
                        .times(MapPaddingAdjustor.BOTTOMSHEET_PADDING_MULTIPLIER_PORTRAIT)
                )
        } else {
            mapView.height
                .minus(
                    bottomSheetHeight
                        .times(MapPaddingAdjustor.BOTTOMSHEET_PADDING_MULTIPLIER_LANDSCAPE)
                )
        }
    }
}
