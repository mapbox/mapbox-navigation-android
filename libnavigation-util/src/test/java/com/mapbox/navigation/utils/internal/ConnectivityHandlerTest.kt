package com.mapbox.navigation.utils.internal

import com.mapbox.common.NetworkStatus
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.Rule
import org.junit.Test

class ConnectivityHandlerTest {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @Test
    fun `not reachable network status channel sends false`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.NOT_REACHABLE)

        verify { mockedNetworkStatusChannel.trySend(false).isSuccess }
    }

    @Test
    fun `reachable via wifi network status channel sends true`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WI_FI)

        verify { mockedNetworkStatusChannel.trySend(true).isSuccess }
    }

    @Test
    fun `reachable via ethernet network status channel sends true`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_ETHERNET)

        verify { mockedNetworkStatusChannel.trySend(true).isSuccess }
    }

    @Test
    fun `reachable via wwan network status channel sends true`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WWAN)

        verify { mockedNetworkStatusChannel.trySend(true).isSuccess }
    }

    @Test
    fun `not reachable logger prints out not reachable`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.NOT_REACHABLE)

        verify {
            logD(
                "MbxConnectivityHandler",
                "NetworkStatus=${NetworkStatus.NOT_REACHABLE}"
            )
        }
    }

    @Test
    fun `reachable via wifi logger prints out reachable via wifi`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WI_FI)

        verify {
            logD(
                "MbxConnectivityHandler",
                "NetworkStatus=${NetworkStatus.REACHABLE_VIA_WI_FI}"
            )
        }
    }

    @Test
    fun `reachable via ethernet logger prints out reachable via ethernet`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_ETHERNET)

        verify {
            logD(
                "MbxConnectivityHandler",
                "NetworkStatus=${NetworkStatus.REACHABLE_VIA_ETHERNET}"
            )
        }
    }

    @Test
    fun `reachable via wwan logger prints out reachable via wwan`() {
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WWAN)

        verify {
            logD(
                "MbxConnectivityHandler",
                "NetworkStatus=${NetworkStatus.REACHABLE_VIA_WWAN}"
            )
        }
    }
}
