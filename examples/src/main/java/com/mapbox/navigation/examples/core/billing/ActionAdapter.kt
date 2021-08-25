package com.mapbox.navigation.examples.core.billing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.R

class ActionAdapter(
    context: Context?,
    private val callback: OnActionButtonClicked
) : RecyclerView.Adapter<ActionAdapter.ActionViewHolder>() {
    private val actionList: MutableList<ActionType> = ArrayList()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    init {
        actionList.add(ActionType.StartSession)
        actionList.add(ActionType.StopSession)
        actionList.add(ActionType.RequestRandomRoute)
        actionList.add(ActionType.Request2LegRoute)
        actionList.add(ActionType.SetAvailableRoute)
        actionList.add(ActionType.RemoveRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val view = inflater.inflate(R.layout.item_animation_list, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        val item = actionList[position]
        holder.bindAnimations(item)
    }

    override fun getItemCount(): Int {
        return actionList.size
    }

    inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var animationButton: Button = itemView.findViewById(R.id.animationButton)
        fun bindAnimations(item: ActionType) {
            animationButton.text = actionList[adapterPosition].name
            animationButton.setOnClickListener { callback.onButtonClicked(item) }
        }
    }

    interface OnActionButtonClicked {
        fun onButtonClicked(actionType: ActionType)
    }
}
