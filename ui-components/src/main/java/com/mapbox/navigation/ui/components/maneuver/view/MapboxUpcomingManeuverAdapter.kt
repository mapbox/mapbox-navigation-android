package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.StepDistance
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.components.databinding.MapboxItemUpcomingManeuversLayoutBinding
import com.mapbox.navigation.ui.components.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default recycler adapter to render upcoming maneuvers for the [RouteLeg].
 * @property context Context
 * @property inflater (android.view.LayoutInflater..android.view.LayoutInflater?)
 * @property upcomingManeuverList MutableList<Maneuver>
 * @constructor
 */
@UiThread
class MapboxUpcomingManeuverAdapter(
    private val context: Context,
) : RecyclerView.Adapter<MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder>() {

    private var turnIconContextThemeWrapper: ContextThemeWrapper? = null
    private var options: ManeuverViewOptions? = null
    private val inflater = LayoutInflater.from(context)
    private val upcomingManeuverList = mutableListOf<Maneuver>()
    private val routeShields = mutableSetOf<RouteShield>()

    /**
     * Binds the given View to the position.
     * @param parent ViewGroup
     * @param viewType Int
     * @return MapboxLaneGuidanceViewHolder
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
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
     * Given a set of [RouteShield], the function maintains a set of [RouteShield] that is used to
     * render the shield on the view
     * @param shields Set<RoadShield>
     */
    fun updateShields(shields: Set<RouteShield>) {
        routeShields.addAll(shields)
    }

    /**
     * Allows you to change the styling of [MapboxTurnIconManeuver].
     * @param contextThemeWrapper ContextThemeWrapper representing desired style
     */
    fun updateUpcomingManeuverIconStyle(contextThemeWrapper: ContextThemeWrapper) {
        this.turnIconContextThemeWrapper = contextThemeWrapper
    }

    /**
     * Allows you to apply styling defined by passed maneuverViewOptions.
     * @param maneuverViewOptions ManeuverViewOptions defining desired style
     */
    fun updateManeuverViewOptions(maneuverViewOptions: ManeuverViewOptions) {
        this.options = maneuverViewOptions
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
    @UiThread
    inner class MapboxUpcomingManeuverViewHolder(
        val viewBinding: MapboxMainManeuverLayoutBinding,
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        /**
         * Invoke the method to bind the maneuver to the view.
         * @param maneuver Maneuver
         */
        fun bindUpcomingManeuver(maneuver: Maneuver) {
            val primary = maneuver.primary
            val secondary = maneuver.secondary
            val stepDistance = maneuver.stepDistance
            updateTurnIconStyle()
            applyOptions()
            drawPrimaryManeuver(primary, routeShields)
            drawSecondaryManeuver(secondary, routeShields)
            drawTotalStepDistance(stepDistance)
            updateTextAppearances()
        }

        private fun updateTurnIconStyle() {
            ifNonNull(turnIconContextThemeWrapper) { contextThemeWrapper ->
                viewBinding.maneuverIcon.updateTurnIconStyle(contextThemeWrapper)
            }
        }

        private fun applyOptions() {
            ifNonNull(options) { options ->
                viewBinding.primaryManeuverText.updateOptions(options.primaryManeuverOptions)
                viewBinding.secondaryManeuverText.updateOptions(options.secondaryManeuverOptions)
            }
        }

        private fun updateTextAppearances() {
            ifNonNull(options?.primaryManeuverOptions?.textAppearance) { appearance ->
                TextViewCompat.setTextAppearance(viewBinding.primaryManeuverText, appearance)
            }
            ifNonNull(options?.secondaryManeuverOptions?.textAppearance) { appearance ->
                TextViewCompat.setTextAppearance(viewBinding.secondaryManeuverText, appearance)
            }
            ifNonNull(options?.stepDistanceTextAppearance) { appearance ->
                TextViewCompat.setTextAppearance(viewBinding.stepDistance, appearance)
            }
        }

        private fun drawPrimaryManeuver(primary: PrimaryManeuver, routeShields: Set<RouteShield>?) {
            viewBinding.primaryManeuverText.renderManeuver(primary, routeShields)
            viewBinding.maneuverIcon.renderPrimaryTurnIcon(primary)
        }

        private fun drawTotalStepDistance(stepDistance: StepDistance) {
            viewBinding.stepDistance.renderTotalStepDistance(stepDistance)
        }

        private fun drawSecondaryManeuver(
            secondary: SecondaryManeuver?,
            routeShields: Set<RouteShield>?,
        ) {
            if (secondary != null) {
                viewBinding.secondaryManeuverText.visibility = VISIBLE
                viewBinding.secondaryManeuverText.renderManeuver(secondary, routeShields)
                updateConstraintsToHaveSecondary()
                viewBinding.primaryManeuverText.maxLines = 1
            } else {
                updateConstraintsToOnlyPrimary()
                viewBinding.secondaryManeuverText.visibility = GONE
                viewBinding.primaryManeuverText.maxLines = 2
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
