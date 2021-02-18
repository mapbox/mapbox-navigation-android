package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.maneuver.databinding.MapboxItemUpcomingManeuversLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.view.MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder

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
        val binding = MapboxItemUpcomingManeuversLayoutBinding.inflate(inflater, parent, false)
        val mainLayoutBinding = MapboxMainManeuverLayoutBinding.bind(binding.root)
        return MapboxUpcomingManeuverViewHolder(mainLayoutBinding)
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
            val currentSize = this.upcomingManeuverList.size
            this.upcomingManeuverList.clear()
            this.upcomingManeuverList.addAll(upcomingManeuvers)
            notifyItemRangeRemoved(0, currentSize)
            notifyItemRangeInserted(0, upcomingManeuvers.size)
        }
    }

    /**
     * Invoke to remove a particular [Maneuver] from the recycler view.
     * @param maneuverToRemove Maneuver
     */
    fun removeManeuver(maneuverToRemove: Maneuver) {
        val index = this.upcomingManeuverList.indexOfFirst {
            it.primary.text == maneuverToRemove.primary.text
        }
        if (index != -1) {
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
     * @property viewBinding
     * @constructor
     */
    inner class MapboxUpcomingManeuverViewHolder(
        val viewBinding: MapboxMainManeuverLayoutBinding
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        /**
         * Invoke the method to bind the maneuver to the view.
         * @param maneuver Maneuver
         */
        fun bindUpcomingManeuver(maneuver: Maneuver) {
            val primary = maneuver.primary
            val secondary = maneuver.secondary
            val totalStepDistance = maneuver.totalManeuverDistance
            if (secondary != null) {
                viewBinding.secondaryManeuverText.render(
                    ManeuverState.ManeuverSecondary.Show
                )
                viewBinding.secondaryManeuverText.render(
                    ManeuverState.ManeuverSecondary.Instruction(secondary)
                )
                updateConstraintsToHaveSecondary()
                viewBinding.primaryManeuverText.maxLines = 1
            } else {
                updateConstraintsToOnlyPrimary()
                viewBinding.secondaryManeuverText.render(
                    ManeuverState.ManeuverSecondary.Hide
                )
                viewBinding.primaryManeuverText.maxLines = 2
            }
            viewBinding.primaryManeuverText.render(
                ManeuverState.ManeuverPrimary.Instruction(primary)
            )
            viewBinding.maneuverIcon.render(
                ManeuverState.ManeuverPrimary.Instruction(primary)
            )
            viewBinding.stepDistance.render(
                ManeuverState.TotalStepDistance(
                    MapboxDistanceFormatter(DistanceFormatterOptions.Builder(context).build()),
                    totalStepDistance.totalDistance
                )
            )
        }

        private fun updateConstraintsToOnlyPrimary() {
            val params = viewBinding.primaryManeuverText.layoutParams
                as ConstraintLayout.LayoutParams
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            viewBinding.root.requestLayout()
        }

        private fun updateConstraintsToHaveSecondary() {
            val params = viewBinding.primaryManeuverText.layoutParams
                as ConstraintLayout.LayoutParams
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = viewBinding.secondaryManeuverText.id
            viewBinding.root.requestLayout()
        }
    }
}
