package com.mapbox.navigation.ui.routealert

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class TollCollectionAlertUiModelTest :
    BuilderTest<TollCollectionAlertUiModel, TollCollectionAlertUiModel.Builder>() {
    override fun getImplementationClass() = TollCollectionAlertUiModel::class

    override fun getFilledUpBuilder() = TollCollectionAlertUiModel.Builder(
        mockk(relaxed = true)
    ).apply {
        tollDescription("tollDescription")
    }

    @Test
    override fun trigger() {
        // see docs
    }
}
