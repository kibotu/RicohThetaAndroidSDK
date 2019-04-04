package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.v21.Http21Connector

class ThetaV(
    override val ip4Address: String = "192.168.1.1"
) : ICamera {

    override var isConnected = false

    override val deviceInfoName = "RICOH THETA V"

    override var httpConnector: HttpConnector? = Http21Connector(ip4Address)
}