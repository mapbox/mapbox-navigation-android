package com.mapbox.services.android.navigation.testapp.example.ui.autocomplete

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import com.mapbox.android.search.autocomplete.AutocompleteAdapter
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.services.android.navigation.testapp.R

class ExampleAutocompleteAdapter(private val context: Context) : AutocompleteAdapter(context) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
    val view = inflateView(convertView, parent)
    val feature = getItem(position)
    return updateViewData(view, feature)
  }

  private fun inflateView(convertView: View?, parent: ViewGroup?): View {
    return if (convertView == null) {
      val inflater = LayoutInflater.from(context)
      inflater.inflate(R.layout.example_autocomplete_list_item, parent, false)
    } else {
      convertView
    }
  }

  private fun updateViewData(view: View, feature: CarmenFeature): View {
    val text = view.findViewById<TextView>(R.id.listItemText)
    text.text = feature.text()

    val address = view.findViewById<TextView>(R.id.listItemAddress)
    if (feature.address().isNullOrEmpty()) {
      address.visibility = GONE
    } else {
      address.text = feature.address()
    }
    return view
  }
}