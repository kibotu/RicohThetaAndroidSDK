package com.exozet.ricohtheta.cameras

import com.exozet.ricohtheta.Theta
import com.exozet.ricohtheta.internal.network.HttpConnector
import com.exozet.ricohtheta.internal.view.MJpegInputStream
import com.exozet.ricohtheta.internal.view.MJpegView
import io.reactivex.Observable

interface ICamera{
    enum class VersionNames {
        Undefined,
        Theta_S,
        Theta_V
    }

    fun connect(ipAddress : String) : HttpConnector
    fun startLiveView(view: MJpegView): Observable<MJpegInputStream>
    val versionName: VersionNames
}