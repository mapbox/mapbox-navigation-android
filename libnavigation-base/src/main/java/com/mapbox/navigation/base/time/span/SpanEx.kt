package com.mapbox.navigation.base.time.span

import android.text.Spannable
import android.text.SpannableStringBuilder

internal fun List<SpanItem>.combineSpan(): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    for (item in this) {
        if (item is TextSpanItem) {
            builder.appendSupport(item.span, item.spanText)
        }
    }
    return builder
}

private fun SpannableStringBuilder.appendSupport(span: Any, spanText: String) {
    this.append(spanText, span, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}
