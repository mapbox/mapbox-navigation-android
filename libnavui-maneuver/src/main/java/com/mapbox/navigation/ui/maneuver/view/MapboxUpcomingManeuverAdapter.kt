package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.view.MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder
import kotlinx.android.synthetic.main.mapbox_main_maneuver_layout.view.*

/**
 * Default recycler adapter to render upcoming maneuvers for the [RouteLeg].
 * @property context Context
 * @property inflater (android.view.LayoutInflater..android.view.LayoutInflater?)
 * @property upcomingManeuverList MutableList<Maneuver>
 * @constructor
 */
class MapboxUpcomingManeuverAdapter(
    private val context: Context
) : RecyclerView.Adapter<MapboxUpcomingManeuverViewHolder>() {

    private val inflater = LayoutInflater.from(context)
    private val upcomingManeuverList = mutableListOf<Maneuver>()

    /**
     * Binds the given View to the position.
     * @param parent ViewGroup
     * @param viewType Int
     * @return MapboxLaneGuidanceViewHolder
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MapboxUpcomingManeuverViewHolder {
        val view = inflater.inflate(R.layout.mapbox_item_upcoming_maneuvers_layout, parent, false)
        return MapboxUpcomingManeuverViewHolder(view)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return Int
     */
    override fun getItemCount(): Int {
        return upcomingManeuverList.size
    }

    /**
     * Invoked by RecyclerView to display the data at the specified position.
     * @param holder MapboxLaneGuidanceViewHolder
     * @param position Int
     */
    override fun onBindViewHolder(holder: MapboxUpcomingManeuverViewHolder, position: Int) {
        val maneuver = upcomingManeuverList[position]
        holder.bindUpcomingManeuver(maneuver)
    }

    /**
     * Invoke to add all upcoming maneuvers to the recycler view.
     * @param upcomingManeuvers List<Maneuver>
     */
    fun addUpcomingManeuvers(upcomingManeuvers: List<Maneuver>) {
        if (upcomingManeuvers.isNotEmpty()) {
            this.upcomingManeuverList.clear()
            this.upcomingManeuverList.addAll(upcomingManeuvers)
            notifyItemRangeInserted(0, upcomingManeuverList.size)
        }
    }

    /**
     * Invoke to remove a particular [Maneuver] from the recycler view.
     * @param maneuverToRemove Maneuver
     */
    fun removeManeuver(maneuverToRemove: Maneuver) {
        if (this.upcomingManeuverList.contains(maneuverToRemove)) {
            val index = this.upcomingManeuverList.indexOf(maneuverToRemove)
            this.upcomingManeuverList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    /**
     * Invoke to remove all the maneuvers from the recycler view.
     */
    fun removeManeuvers() {
        if (this.upcomingManeuverList.isNotEmpty()) {
            this.upcomingManeuverList.clear()
            notifyDataSetChanged()
        }
    }

    /**
     * View Holder defined for the [RecyclerView.Adapter]
     * @property view View
     * @constructor
     */
    inner class MapboxUpcomingManeuverViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        /**
         * Invoke the method to bind the maneuver to the view.
         * @param maneuver Maneuver
         */
        fun bindUpcomingManeuver(maneuver: Maneuver) {
            val primary = maneuver.primary
            val secondary = maneuver.secondary
            val totalStepDistance = maneuver.totalManeuverDistance
            if (secondary != null) {
                view.secondaryManeuverText.render(ManeuverState.ManeuverSecondary.Show)
                view.secondaryManeuverText.render(
                    ManeuverState.ManeuverSecondary.Instruction(secondary)
                )
                updateConstraintsToHaveSecondary()
                view.primaryManeuverText.maxLines = 1
            } else {
                updateConstraintsToOnlyPrimary()
                view.secondaryManeuverText.render(ManeuverState.ManeuverSecondary.Hide)
                view.primaryManeuverText.maxLines = 2
            }
            view.primaryManeuverText.render(ManeuverState.ManeuverPrimary.Instruction(primary))
            view.maneuverIcon.render(ManeuverState.ManeuverPrimary.Instruction(primary))
            view.stepDistance.render(
                ManeuverState.TotalStepDistance(
                    MapboxDistanceFormatter(DistanceFormatterOptions.Builder(context).build()),
                    totalStepDistance.totalDistance
                )
            )
        }

        private fun updateConstraintsToOnlyPrimary() {
            val params = view.primaryManeuverText.layoutParams as ConstraintLayout.LayoutParams
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            view.requestLayout()
        }

        private fun updateConstraintsToHaveSecondary() {
            val params = view.primaryManeuverText.layoutParams as ConstraintLayout.LayoutParams
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = view.secondaryManeuverText.id
            view.requestLayout()
        }
    }
}
