package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.maneuver.databinding.MapboxItemUpcomingManeuversLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.TotalManeuverDistance
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
        val diffCallback = MapboxManeuverDiffCallback(upcomingManeuverList, upcomingManeuvers)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        upcomingManeuverList.clear()
        upcomingManeuverList.addAll(upcomingManeuvers)
        diffResult.dispatchUpdatesTo(this)
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
            drawSecondaryManeuver(secondary)
            drawPrimaryManeuver(primary)
            drawTotalStepDistance(totalStepDistance)
        }

        private fun drawPrimaryManeuver(primary: PrimaryManeuver) {
            viewBinding.primaryManeuverText.render(primary)
            viewBinding.maneuverIcon.renderPrimaryTurnIcon(primary)
        }

        private fun drawTotalStepDistance(totalStepDistance: TotalManeuverDistance) {
            viewBinding.stepDistance.render(
                StepDistance(
                    MapboxDistanceFormatter(DistanceFormatterOptions.Builder(context).build()),
                    totalStepDistance.totalDistance
                )
            )
        }

        private fun drawSecondaryManeuver(secondary: SecondaryManeuver?) {
            if (secondary != null) {
                viewBinding.secondaryManeuverText.visibility = VISIBLE
                viewBinding.secondaryManeuverText.render(secondary)
                updateConstraintsToHaveSecondary()
                viewBinding.primaryManeuverText.isSingleLine = true
            } else {
                updateConstraintsToOnlyPrimary()
                viewBinding.secondaryManeuverText.visibility = GONE
                viewBinding.primaryManeuverText.isSingleLine = false
            }
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
