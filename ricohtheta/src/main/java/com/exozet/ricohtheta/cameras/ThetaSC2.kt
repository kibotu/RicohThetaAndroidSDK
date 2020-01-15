package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.network.v2.Http2Connector


class ThetaSC2(
    override val ip4Address: String = "192.168.1.1"
) : ICamera {

    override var isConnected = false

    override val deviceInfoName = "RICOH THETA SC2"

    override var httpConnector: HttpConnector? = Http2Connector(ip4Address)
}