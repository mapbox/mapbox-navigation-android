package com.mapbox.navigation.examples.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.R

typealias HistoryFileItemClicked = (ReplayPath) -> Unit

class HistoryFileAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var data: List<ReplayPath> = listOf()
    var itemClicked: HistoryFileItemClicked? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_files_list_item, parent, false)
        return HistoryFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val dataValue = data[position]
        val historyHolder = holder as HistoryFileViewHolder
        historyHolder.textViewTop.text = dataValue.title
        historyHolder.textViewBottom.text = dataValue.description
        historyHolder.itemView.setOnClickListener {
            itemClicked?.invoke(dataValue)
        }
    }

    override fun getItemCount() = data.size
}

class HistoryFileViewHolder(topView: View) : RecyclerView.ViewHolder(topView) {
    val textViewTop: TextView = topView.findViewById(R.id.textViewTop)
    val textViewBottom: TextView = topView.findViewById(R.id.textViewBottom)
}
