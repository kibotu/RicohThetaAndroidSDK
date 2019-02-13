package com.exozet.ricohtheta

import com.exozet.ricohtheta.internal.network.v2.Http2Connector


object ThetaS : Theta(){


    override fun connect(ip4Address: String) = Http2Connector(ip4Address)




    override var versionName: VersionNames = Theta.VersionNames.Theta_S

}