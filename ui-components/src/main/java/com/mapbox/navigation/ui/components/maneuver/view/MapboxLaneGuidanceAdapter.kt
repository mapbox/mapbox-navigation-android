package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.tripdata.maneuver.api.MapboxLaneIconsApi
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.databinding.MapboxItemLaneGuidanceLayoutBinding
import com.mapbox.navigation.ui.components.maneuver.view.MapboxLaneGuidanceAdapter.MapboxLaneGuidanceViewHolder

/**
 * Default recycler adapter to render lanes for the upcoming turn.
 * @property context Context
 * @property inflater (android.view.LayoutInflater..android.view.LayoutInflater?)
 * @property laneIndicatorList MutableList<LaneIndicator>
 * @constructor
 */
@UiThread
class MapboxLaneGuidanceAdapter(
    private val context: Context,
) : RecyclerView.Adapter<MapboxLaneGuidanceViewHolder>() {

    private val inflater = LayoutInflater.from(context)
    private val laneIndicatorList = mutableListOf<LaneIndicator>()
    private val laneApi = MapboxLaneIconsApi()
    private var wrapper = ContextThemeWrapper(context, R.style.MapboxStyleTurnIconManeuver)

    /**
     * Binds the given View to the position.
     * @param parent ViewGroup
     * @param viewType Int
     * @return MapboxLaneGuidanceViewHolder
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MapboxLaneGuidanceViewHolder {
        val binding = MapboxItemLaneGuidanceLayoutBinding.inflate(inflater, parent, false)
        return MapboxLaneGuidanceViewHolder(binding)
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
     */
    fun addLanes(laneIndicatorList: List<LaneIndicator>) {
        if (laneIndicatorList.isNotEmpty()) {
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
            this.laneIndicatorList.clear()
            notifyDataSetChanged()
        }
    }

    /**
     * Invoke to change how the turn icons would look in the lane guidance view.
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        this.wrapper = ContextThemeWrapper(context, style)
        notifyDataSetChanged()
    }

    /**
     * View Holder defined for the [RecyclerView.Adapter]
     * @property viewBinding
     * @constructor
     */
    inner class MapboxLaneGuidanceViewHolder(
        private val viewBinding: MapboxItemLaneGuidanceLayoutBinding,
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        /**
         * Invoke the method to bind the lane to the view.
         * @param laneIndicator LaneIndicator
         */
        @UiThread
        fun bindLaneIndicator(laneIndicator: LaneIndicator) {
            val laneIcon = laneApi.getTurnLane(laneIndicator)
            viewBinding.itemLaneGuidance.renderLane(
                laneIcon,
                wrapper,
            )
        }
    }
}
