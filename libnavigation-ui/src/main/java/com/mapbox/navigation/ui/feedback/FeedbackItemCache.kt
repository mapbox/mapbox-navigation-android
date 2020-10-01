package com.mapbox.navigation.ui.feedback

import java.util.LinkedList
import java.util.Queue

/**
 * This class handles the queuing of [FeedbackItem] that can be sent
 * at a later time.
 */
class FeedbackItemCache {

    private var feedbackItemQueue: Queue<FeedbackItem>? = null

    companion object{
        fun newInstance(): FeedbackItemCache = FeedbackItemCache()
    }

    init {
        feedbackItemQueue = LinkedList<FeedbackItem>()
    }

    fun addNewFeedbackItem(feedbackItem: FeedbackItem) {
        feedbackItemQueue?.add(feedbackItem)
    }

    fun getFeedbackItems(): List<FeedbackItem>? {
        return feedbackItemQueue?.toList()
    }

    fun removeAllItems() {
        feedbackItemQueue?.clear()
    }
}
