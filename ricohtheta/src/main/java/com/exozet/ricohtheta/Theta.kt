package com.exozet.ricohtheta

import android.net.Uri
import com.exozet.ricohtheta.internal.view.MJpegView
import io.reactivex.Observable

open abstract class Theta {


    enum class VersionNames {
        Undefined,
        Theta_S,
        Theta_V
    }

    open fun connect() {}
    open fun disconnect() {}
    val isConnected: Boolean
        get() {
            return true
        }


    open val versionName: VersionNames = Theta.VersionNames.Undefined

    fun takePicture(): Uri {
        return Uri.EMPTY
    }//(Observable machen -> Stream + delete image )
    //fun getThumbnail() : Uri{ return Uri.EMPTY}

    fun startLiveView(view: MJpegView) {}
    fun stopLiveView() {}
}