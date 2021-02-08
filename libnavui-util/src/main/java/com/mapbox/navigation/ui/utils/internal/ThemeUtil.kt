package com.mapbox.navigation.ui.utils.internal

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AnyRes

object ThemeUtil {

    fun retrieveAttrResourceId(context: Context, attrId: Int, defaultResId: Int): Int {
        val outValue: TypedValue = resolveAttributeFromId(context, attrId)
        return if (isValid(outValue.resourceId)) {
            outValue.resourceId
        } else {
            defaultResId
        }
    }

    private fun resolveAttributeFromId(context: Context, resId: Int): TypedValue {
        val outValue = TypedValue()
        context.theme.resolveAttribute(resId, outValue, true)
        return outValue
    }

    private fun isValid(@AnyRes resId: Int): Boolean {
        return resId != -1 && resId and -0x1000000 != 0 && resId and 0x00ff0000 != 0
    }
}
