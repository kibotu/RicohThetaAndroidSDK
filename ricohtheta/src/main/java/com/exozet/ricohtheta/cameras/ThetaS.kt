package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.v2.Http2Connector


object ThetaS : ICamera{

    override fun connection(ip4Address: String) = Http2Connector(ip4Address)

    override var deviceInfoName: String = "RICOH THETA S"
}