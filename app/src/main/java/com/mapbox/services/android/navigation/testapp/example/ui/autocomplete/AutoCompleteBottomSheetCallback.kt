package com.mapbox.services.android.navigation.testapp.example.ui.autocomplete

import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback
import android.view.View
import com.mapbox.services.android.navigation.testapp.example.ui.ExamplePresenter

internal class AutoCompleteBottomSheetCallback(private val presenter: ExamplePresenter): BottomSheetCallback() {

  override fun onSlide(bottomSheet: View, slideOffset: Float) {
    // No impl
  }

  override fun onStateChanged(bottomSheet: View, newState: Int) {
    presenter.onAutocompleteBottomSheetStateChange(newState)
  }
}