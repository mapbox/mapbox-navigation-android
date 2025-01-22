package com.mapbox.navigation.copilot

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DataClassGeneratedFunctionsTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            SearchResultsEvent::class.java,
            SearchResultUsedEvent::class.java,
        ).forEach {
            EqualsVerifier.forClass(it)
                .withIgnoredFields("eventDTO")
                .verify()

            ToStringVerifier.forClass(it)
                .withIgnoredFields("eventDTO")
                .verify()
        }

        listOf(
            HistoryPoint::class.java,
            HistoryRoutablePoint::class.java,
            HistorySearchResult::class.java,
            SearchResultUsed::class.java,
            SearchResults::class.java,
        ).forEach {
            EqualsVerifier.forClass(it).verify()
            ToStringVerifier.forClass(it).verify()
        }
    }
}
