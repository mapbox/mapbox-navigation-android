package com.mapbox.navigation.ui.maneuver.view

import androidx.recyclerview.widget.DiffUtil
import com.mapbox.navigation.ui.maneuver.model.Maneuver

/**
 * A utility used to calculate the difference between two lists of upcoming maneuvers.
 * @property oldList List<Maneuver>
 * @property newList List<Maneuver>
 * @constructor
 */
internal class MapboxManeuverDiffCallback(
    private val oldList: List<Maneuver>,
    private val newList: List<Maneuver>
) : DiffUtil.Callback() {

    /**
     * Returns the size of old list.
     * @return Int
     */
    override fun getOldListSize(): Int = oldList.size

    /**
     * Returns the size of new list.
     * @return Int
     */
    override fun getNewListSize(): Int = newList.size

    /**
     * Called by the DiffUtil to decide whether two object represent the same Item.
     * @param oldItemPosition Int
     * @param newItemPosition Int
     * @return Boolean
     */
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].primary.text === newList[newItemPosition].primary.text
    }

    /**
     * Called by the DiffUtil when it wants to check whether two items have the same data.
     * @param oldItemPosition Int
     * @param newItemPosition Int
     * @return Boolean
     */
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    /**
     * When [areItemsTheSame] returns true for two items and [areContentsTheSame] returns false
     * for them, DiffUtil calls this method to get a payload about the change.
     * @param oldItemPosition Int
     * @param newItemPosition Int
     * @return Any?
     */
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
