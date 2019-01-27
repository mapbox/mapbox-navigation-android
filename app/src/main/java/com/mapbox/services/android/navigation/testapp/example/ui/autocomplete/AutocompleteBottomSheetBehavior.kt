package com.mapbox.services.android.navigation.testapp.example.ui.autocomplete

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class AutocompleteBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {

  constructor() : super()

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
    return false
  }

  override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
    return false
  }
}