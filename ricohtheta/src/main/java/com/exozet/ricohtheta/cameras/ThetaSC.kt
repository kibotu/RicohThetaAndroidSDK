package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.v2.Http2Connector


class ThetaSC(
    override val ip4Address: String = "192.168.1.1"
) : ICamera {

    override var isConnected = false

    override val deviceInfoName = "RICOH THETA SC"

    override var httpConnector: HttpConnector? = Http2Connector(ip4Address)
}