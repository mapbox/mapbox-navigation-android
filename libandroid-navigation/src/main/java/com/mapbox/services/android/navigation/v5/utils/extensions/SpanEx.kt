package com.mapbox.services.android.navigation.v5.utils.extensions

import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import com.mapbox.services.android.navigation.v5.utils.span.SpanItem
import com.mapbox.services.android.navigation.v5.utils.span.TextSpanItem

fun List<SpanItem>.combineSpan(): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    for (item in this) {
        if (item is TextSpanItem) {
            builder.appendSupport(item.span, item.spanText)
        }
    }
    return builder
}

private fun SpannableStringBuilder.appendSupport(span: Any, spanText: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.append(spanText, span, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else {
        this.append(spanText)
    }
}
