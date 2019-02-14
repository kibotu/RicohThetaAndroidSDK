package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.internal.network.v2.Http2Connector
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.ricohtheta.internal.view.MJpegView
import io.reactivex.Observable


object ThetaS : ICamera{

    override fun connect(ip4Address: String) = Http2Connector(ip4Address)


    override var versionName: ICamera.VersionNames = ICamera.VersionNames.Theta_S
}