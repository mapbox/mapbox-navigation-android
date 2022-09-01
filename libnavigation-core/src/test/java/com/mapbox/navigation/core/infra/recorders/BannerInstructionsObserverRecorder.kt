package com.mapbox.navigation.core.infra.recorders

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver

class BannerInstructionsObserverRecorder : BannerInstructionsObserver {

    private val _records = mutableListOf<BannerInstructions>()
    val records: List<BannerInstructions> get() = _records

    override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
        _records.add(bannerInstructions)
    }
}
