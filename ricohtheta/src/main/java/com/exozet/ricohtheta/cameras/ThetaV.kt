package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.internal.network.v21.Http21Connector

object ThetaV : ICamera{

    override fun connection(ip4Address: String) = Http21Connector(ip4Address)

    override var deviceInfoName: String = "RICOH THETA V"

}
