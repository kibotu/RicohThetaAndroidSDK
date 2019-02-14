package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.internal.network.v21.Http21Connector
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.ricohtheta.internal.view.MJpegView
import io.reactivex.Observable

object ThetaV : ICamera{
    override fun startLiveView(view: MJpegView): Observable<MJpegInputStream> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var versionName: ICamera.VersionNames = ICamera.VersionNames.Theta_V

    override fun connect(ip4Address: String) = Http21Connector(ip4Address)

}