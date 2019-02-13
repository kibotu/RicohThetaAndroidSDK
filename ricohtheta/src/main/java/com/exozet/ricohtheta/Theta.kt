package com.exozet.ricohtheta

import android.net.Uri
import com.exozet.ricohtheta.internal.view.MJpegView

object Theta {


    enum class VersionNames {
        Theta_S,
        Theta_V
    }

    fun connect() {}
    fun disconnect() {}
    val isConnected: Boolean
        get() {
            return true
        }


    val versionName: VersionNames = Theta.VersionNames.Theta_S

    fun takePicture(): Uri {
        return Uri.EMPTY
    }//(Observable machen -> Stream + delete image )
    //fun getThumbnail() : Uri{ return Uri.EMPTY}

    fun startLiveView(view: MJpegView) {}
    fun stopLiveView() {}
}