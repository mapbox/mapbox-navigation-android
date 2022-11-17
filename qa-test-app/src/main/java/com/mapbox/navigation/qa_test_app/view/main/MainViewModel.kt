package com.mapbox.navigation.qa_test_app.view.main

import androidx.lifecycle.ViewModel
import com.mapbox.navigation.qa_test_app.domain.TestActivityDescription
import com.mapbox.navigation.qa_test_app.domain.TestActivitySuite
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PageInfo(val title: String, val category: String)

class MainViewModel : ViewModel() {

    private val _didSelectItemEvent = eventFlow<TestActivityDescription>()
    val didSelectItemEvent: Flow<TestActivityDescription> = _didSelectItemEvent.asSharedFlow()

    private val _didSelectInfoEvent = eventFlow<TestActivityDescription>()
    val didSelectInfoEvent: Flow<TestActivityDescription> = _didSelectInfoEvent.asSharedFlow()

    val pages: List<PageInfo> = listOf(
        PageInfo("All", TestActivitySuite.CATEGORY_NONE),
        PageInfo("Drop-In UI", TestActivitySuite.CATEGORY_DROP_IN),
        PageInfo("Component Installer", TestActivitySuite.CATEGORY_COMPONENTS)
    )

    fun getActivitiesList(category: String): List<TestActivityDescription> {
        return TestActivitySuite.getTestActivities(category)
    }

    fun onSelectItem(item: TestActivityDescription) {
        _didSelectItemEvent.tryEmit(item)
    }

    fun onSelectInfoIcon(item: TestActivityDescription) {
        _didSelectInfoEvent.tryEmit(item)
    }
}

internal fun <T> eventFlow(): MutableSharedFlow<T> =
    MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
