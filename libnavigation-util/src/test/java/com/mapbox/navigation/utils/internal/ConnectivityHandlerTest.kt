package com.mapbox.navigation.utils.internal

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.NetworkStatus
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.Test

class ConnectivityHandlerTest {

    @Test
    fun `not reachable network status channel sends false`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.NOT_REACHABLE)

        verify { mockedNetworkStatusChannel.trySend(false).isSuccess }
    }

    @Test
    fun `reachable via wifi network status channel sends true`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WI_FI)

        verify { mockedNetworkStatusChannel.trySend(true).isSuccess }
    }

    @Test
    fun `reachable via ethernet network status channel sends true`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_ETHERNET)

        verify { mockedNetworkStatusChannel.trySend(true).isSuccess }
    }

    @Test
    fun `reachable via wwan network status channel sends true`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WWAN)

        verify { mockedNetworkStatusChannel.trySend(true).isSuccess }
    }

    @Test
    fun `not reachable logger prints out not reachable`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.NOT_REACHABLE)

        verify {
            mockedLogger.d(
                Tag("MbxConnectivityHandler"),
                Message("NetworkStatus=${NetworkStatus.NOT_REACHABLE}")
            )
        }
    }

    @Test
    fun `reachable via wifi logger prints out reachable via wifi`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WI_FI)

        verify {
            mockedLogger.d(
                Tag("MbxConnectivityHandler"),
                Message("NetworkStatus=${NetworkStatus.REACHABLE_VIA_WI_FI}")
            )
        }
    }

    @Test
    fun `reachable via ethernet logger prints out reachable via ethernet`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_ETHERNET)

        verify {
            mockedLogger.d(
                Tag("MbxConnectivityHandler"),
                Message("NetworkStatus=${NetworkStatus.REACHABLE_VIA_ETHERNET}")
            )
        }
    }

    @Test
    fun `reachable via wwan logger prints out reachable via wwan`() {
        val mockedLogger: Logger = mockk(relaxUnitFun = true)
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedLogger, mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WWAN)

        verify {
            mockedLogger.d(
                Tag("MbxConnectivityHandler"),
                Message("NetworkStatus=${NetworkStatus.REACHABLE_VIA_WWAN}")
            )
        }
    }
}
