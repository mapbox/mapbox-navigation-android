package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.view.MapboxLaneGuidanceAdapter.MapboxLaneGuidanceViewHolder
import com.mapbox.navigation.ui.utils.internal.ThemeUtil
import kotlinx.android.synthetic.main.mapbox_item_lane_guidance.view.*

/**
 * Default recycler adapter to render lanes for the upcoming turn.
 * @property context Context
 * @property activeDirection String?
 * @property inflater (android.view.LayoutInflater..android.view.LayoutInflater?)
 * @property laneIndicatorList MutableList<LaneIndicator>
 * @constructor
 */
class MapboxLaneGuidanceAdapter(
    private val context: Context,
) : RecyclerView.Adapter<MapboxLaneGuidanceViewHolder>() {

    // TODO: Remove when the migration to valhalla is complete to be able to use
    //  component.active_directions
    private var activeDirection: String? = null
    private val inflater = LayoutInflater.from(context)
    private val laneIndicatorList = mutableListOf<LaneIndicator>()
    @StyleRes private var laneGuidanceStyle: Int = R.style.MapboxStyleTurnIconManeuver

    /**
     * Binds the given View to the position.
     * @param parent ViewGroup
     * @param viewType Int
     * @return MapboxLaneGuidanceViewHolder
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MapboxLaneGuidanceViewHolder {
        val view = inflater.inflate(R.layout.mapbox_item_lane_guidance, parent, false)
        return MapboxLaneGuidanceViewHolder(view)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return Int
     */
    override fun getItemCount(): Int {
        return laneIndicatorList.size
    }

    /**
     * Invoked by RecyclerView to display the data at the specified position.
     * @param holder MapboxLaneGuidanceViewHolder
     * @param position Int
     */
    override fun onBindViewHolder(holder: MapboxLaneGuidanceViewHolder, position: Int) {
        val laneIndicator = laneIndicatorList[position]
        holder.bindLaneIndicator(laneIndicator)
    }

    /**
     * Invoke to add all the lanes to the recycler view.
     * @param laneIndicatorList List<LaneIndicator>
     * @param activeDirection String?
     */
    fun addLanes(laneIndicatorList: List<LaneIndicator>, activeDirection: String?) {
        if (laneIndicatorList.isNotEmpty()) {
            this.activeDirection = activeDirection
            this.laneIndicatorList.clear()
            this.laneIndicatorList.addAll(laneIndicatorList)
            notifyDataSetChanged()
        }
    }

    /**
     * Invoke to remove all the lanes from the recycler view.
     */
    fun removeLanes() {
        if (this.laneIndicatorList.isNotEmpty()) {
            this.activeDirection = null
            this.laneIndicatorList.clear()
            notifyDataSetChanged()
        }
    }

    /**
     * Invoke to change how the turn icons would look in the lane guidance view.
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        this.laneGuidanceStyle =
            ThemeUtil.retrieveAttrResourceId(context, style, R.style.MapboxStyleTurnIconManeuver)
    }

    /**
     * View Holder defined for the [RecyclerView.Adapter]
     * @property view View
     * @constructor
     */
    inner class MapboxLaneGuidanceViewHolder(
        val view: View
    ) : RecyclerView.ViewHolder(view) {

        /**
         * Invoke the method to bind the lane to the view.
         * @param laneIndicator LaneIndicator
         */
        fun bindLaneIndicator(laneIndicator: LaneIndicator) {
            view.itemLaneGuidance.renderLane(
                laneIndicator,
                activeDirection,
                ContextThemeWrapper(view.context, laneGuidanceStyle)
            )
        }
    }
}
