package com.mapbox.navigation.utils.internal

import com.mapbox.common.NetworkStatus
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Before
import org.junit.Test

class ConnectivityHandlerTest {

    private val originalLogger = LoggerProvider.getLoggerFrontend()
    private val mockLogger = mockk<LoggerFrontend>(relaxed = true)

    @Before
    fun setup() {
        LoggerProvider.setLoggerFrontend(mockLogger)
    }

    @After
    fun tearDown() {
        LoggerProvider.setLoggerFrontend(originalLogger)
    }

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
        val logger = mockLogger
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.NOT_REACHABLE)

        verify {
            logger.logD(
                "NetworkStatus=${NetworkStatus.NOT_REACHABLE}",
                "ConnectivityHandler",
            )
        }
    }

    @Test
    fun `reachable via wifi logger prints out reachable via wifi`() {
        val logger = mockLogger
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WI_FI)

        verify {
            logger.logD(
                "NetworkStatus=${NetworkStatus.REACHABLE_VIA_WI_FI}",
                "ConnectivityHandler",
            )
        }
    }

    @Test
    fun `reachable via ethernet logger prints out reachable via ethernet`() {
        val logger = mockLogger
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_ETHERNET)

        verify {
            logger.logD(
                "NetworkStatus=${NetworkStatus.REACHABLE_VIA_ETHERNET}",
                "ConnectivityHandler",
            )
        }
    }

    @Test
    fun `reachable via wwan logger prints out reachable via wwan`() {
        val logger = mockLogger
        val mockedNetworkStatusChannel: Channel<Boolean> = mockk(relaxed = true)
        val connectivityHandler = ConnectivityHandler(mockedNetworkStatusChannel)

        connectivityHandler.run(NetworkStatus.REACHABLE_VIA_WWAN)

        verify {
            logger.logD(
                "NetworkStatus=${NetworkStatus.REACHABLE_VIA_WWAN}",
                "ConnectivityHandler",
            )
        }
    }
}
