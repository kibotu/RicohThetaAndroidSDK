package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.v2.Http2Connector


class ThetaS : ICamera {

    override val deviceInfoName = "RICOH THETA S"

    override val isConnected: Boolean = false

    override var httpConnector: HttpConnector? = null

    override fun connection(ip4Address: String): HttpConnector {
        httpConnector = Http2Connector(ip4Address)
        return httpConnector!!
    }

    override fun disconnect() {
        // httpConnector?.disconnect()
    }
}