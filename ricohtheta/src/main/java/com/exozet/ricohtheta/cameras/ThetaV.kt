package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.v21.Http21Connector

class ThetaV : ICamera {

    override val deviceInfoName = "RICOH THETA V"

    override val isConnected: Boolean = false

    override var httpConnector: HttpConnector? = null

    override fun connection(ip4Address: String): HttpConnector {
        httpConnector = Http21Connector(ip4Address)
        return httpConnector!!
    }

    override fun disconnect(): Boolean {
        return httpConnector?.disconnect() ?: false
    }
}