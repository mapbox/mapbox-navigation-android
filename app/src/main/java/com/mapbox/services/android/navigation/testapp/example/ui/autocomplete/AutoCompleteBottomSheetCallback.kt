package com.mapbox.services.android.navigation.testapp.example.ui.autocomplete

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.services.android.navigation.testapp.example.ui.ExamplePresenter

internal class AutoCompleteBottomSheetCallback(private val presenter: ExamplePresenter): BottomSheetBehavior.BottomSheetCallback() {

  override fun onSlide(bottomSheet: View, slideOffset: Float) {
    // No impl
  }

  override fun onStateChanged(bottomSheet: View, newState: Int) {
    presenter.onAutocompleteBottomSheetStateChange(newState)
  }
}