package com.mapbox.navigation.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet

/**
 * Extension function returning styled attribute information
 * @receiver Context
 * @param attributeSet AttributeSet?
 * @param styledArray IntArray
 * @param block [@kotlin.ExtensionFunctionType] Function1<TypedArray, Unit>
 */
@SuppressLint("Recycle")
fun Context.getStyledAttributes(
    attributeSet: AttributeSet?,
    styledArray: IntArray,
    block: TypedArray.() -> Unit
) = this.obtainStyledAttributes(attributeSet, styledArray, 0, 0).use(block)
