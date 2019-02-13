package com.exozet.ricohtheta

import com.exozet.ricohtheta.internal.network.v21.Http21Connector

object ThetaV : Theta(){

    override var versionName: VersionNames = Theta.VersionNames.Theta_V

    override fun connect(ip4Address: String) = Http21Connector(ip4Address)

}