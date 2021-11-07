package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.ui.maneuver.databinding.MapboxItemUpcomingManeuversLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.view.MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder
import com.mapbox.navigation.utils.internal.ifNonNull

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

    @StyleRes private var stepDistanceAppearance: Int? = null
    @StyleRes private var primaryManeuverAppearance: Int? = null
    @StyleRes private var secondaryManeuverAppearance: Int? = null
    private val inflater = LayoutInflater.from(context)
    private val upcomingManeuverList = mutableListOf<Maneuver>()
    private val maneuverShieldMap = mutableMapOf<String, List<RoadShield>>()

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
     * Given a [Map] of id to [RoadShield], the function maintains a map of id to [RoadShield] that
     * is used to render the shield on the view
     * @param shieldMap Map<String, RoadShield?>
     */
    @Deprecated(
        message = "The method can only render one shield if an instruction has multiple shields",
        replaceWith = ReplaceWith("updateShields(shields)")
    )
    fun updateRoadShields(shieldMap: Map<String, RoadShield?>) {
        val shields = hashMapOf<String, List<RoadShield>>()
        shieldMap.forEach {
            val value = it.value
            shields[it.key] = if (value != null) {
                listOf(value)
            } else {
                listOf()
            }
        }
        updateShields(shields)
    }

    /**
     * Given a [Map] of id to list of [RoadShield], the function maintains a map of id to list of
     * [RoadShield] that is used to render the shield on the view
     * @param shields Map<String, List<RoadShield>>
     */
    fun updateShields(shields: Map<String, List<RoadShield>>) {
        maneuverShieldMap.putAll(shields)
    }

    /**
     * Allows you to change the text appearance of step distance text in upcoming maneuver list.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateUpcomingManeuverStepDistanceTextAppearance(@StyleRes style: Int) {
        stepDistanceAppearance = style
    }

    /**
     * Allows you to change the text appearance of primary maneuver text in upcoming maneuver list.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateUpcomingPrimaryManeuverTextAppearance(@StyleRes style: Int) {
        primaryManeuverAppearance = style
    }

    /**
     * Allows you to change the text appearance of secondary maneuver text in upcoming maneuver list.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateUpcomingSecondaryManeuverTextAppearance(@StyleRes style: Int) {
        secondaryManeuverAppearance = style
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
            val stepDistance = maneuver.stepDistance
            val primaryId = primary.id
            val secondaryId = secondary?.id
            drawPrimaryManeuver(primary, maneuverShieldMap[primaryId])
            drawSecondaryManeuver(secondary, maneuverShieldMap[secondaryId])
            drawTotalStepDistance(stepDistance)
            updateStepDistanceTextAppearance()
            updateUpcomingPrimaryManeuverTextAppearance()
            updateUpcomingSecondaryManeuverTextAppearance()
        }

        private fun updateUpcomingPrimaryManeuverTextAppearance() {
            ifNonNull(primaryManeuverAppearance) { appearance ->
                TextViewCompat.setTextAppearance(viewBinding.primaryManeuverText, appearance)
            }
        }

        private fun updateUpcomingSecondaryManeuverTextAppearance() {
            ifNonNull(secondaryManeuverAppearance) { appearance ->
                TextViewCompat.setTextAppearance(viewBinding.secondaryManeuverText, appearance)
            }
        }

        private fun updateStepDistanceTextAppearance() {
            ifNonNull(stepDistanceAppearance) { appearance ->
                TextViewCompat.setTextAppearance(viewBinding.stepDistance, appearance)
            }
        }

        private fun drawPrimaryManeuver(primary: PrimaryManeuver, roadShields: List<RoadShield>?) {
            viewBinding.primaryManeuverText.renderManeuver(primary, roadShields)
            viewBinding.maneuverIcon.renderPrimaryTurnIcon(primary)
        }

        private fun drawTotalStepDistance(stepDistance: StepDistance) {
            viewBinding.stepDistance.renderTotalStepDistance(stepDistance)
        }

        private fun drawSecondaryManeuver(
            secondary: SecondaryManeuver?,
            roadShields: List<RoadShield>?
        ) {
            if (secondary != null) {
                viewBinding.secondaryManeuverText.visibility = VISIBLE
                viewBinding.secondaryManeuverText.renderManeuver(secondary, roadShields)
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
